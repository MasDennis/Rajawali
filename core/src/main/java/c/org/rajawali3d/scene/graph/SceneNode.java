package c.org.rajawali3d.scene.graph;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import c.org.rajawali3d.annotations.RequiresReadLock;
import c.org.rajawali3d.annotations.RequiresWriteLock;
import c.org.rajawali3d.bounds.AABB;
import c.org.rajawali3d.intersection.Intersector;
import c.org.rajawali3d.object.RenderableObject;
import c.org.rajawali3d.transform.Transformable;
import c.org.rajawali3d.transform.Transformation;
import c.org.rajawali3d.transform.Transformer;
import net.jcip.annotations.ThreadSafe;
import org.rajawali3d.math.Matrix4;
import org.rajawali3d.math.vector.Vector3;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Lock;

/**
 * Container class for items which are added to a scene. Scene objects which need to be treated as a group can all be
 * added to a single {@link SceneNode}. Except where noted, this class is thread safe and protected via a Reentrant
 * Read-Write system.
 *
 * @author Jared Woolston (Jared.Woolston@gmail.com)
 */
@ThreadSafe
public class SceneNode implements NodeParent, NodeMember, Transformable {

    @NonNull
    private final Transformation transformation = new Transformation();

    @NonNull
    private final Vector3 maxBound = new Vector3();

    @NonNull
    private final Vector3 minBound = new Vector3();

    @VisibleForTesting
    @NonNull
    final List<SceneNode> children = new ArrayList<>();

    @VisibleForTesting
    @NonNull
    final List<NodeMember> members = new ArrayList<>();

    @Nullable
    protected NodeParent parent;

    @Nullable
    protected Lock currentlyHeldWriteLock;

    @Nullable
    protected Lock currentlyHeldReadLock;

    protected volatile boolean visible = true;

    @NonNull
    protected Transformation getTransformation() {
        return transformation;
    }

    @RequiresReadLock
    @NonNull
    @Override
    public Vector3 getMaxBound() {
        return maxBound;
    }

    @RequiresReadLock
    @NonNull
    @Override
    public Vector3 getMinBound() {
        return minBound;
    }

    @RequiresWriteLock
    public void recalculateBounds() {
        recalculateBounds(false);
    }

    @Override
    public void requestTransformations(@NonNull Transformer transformer) throws InterruptedException {
        // Acquire write lock
        currentlyHeldWriteLock = acquireWriteLock();
        try {
            // Let the transformer do what it wants
            transformer.transform(getTransformation());
            // Update the scene graph to take account of any transformations we made.
            updateGraph();
        } finally {
            // Release write lock
            releaseWriteLock();
        }
    }

    @Nullable
    @Override
    public Lock acquireWriteLock() throws InterruptedException {
        // If this node has been added to a graph, we need to ask for a lock, otherwise we can continue.
        if (parent != null) {
            //noinspection ConstantConditions
            return parent.acquireWriteLock();
        } else {
            return null;
        }
    }

    @Nullable
    @Override
    public Lock acquireReadLock() throws InterruptedException {
        // If this node has been added to a graph, we need to ask for a lock, otherwise we can continue.
        if (parent != null) {
            //noinspection ConstantConditions
            return parent.acquireReadLock();
        } else {
            return null;
        }
    }

    @Override
    public void setParent(@Nullable NodeParent parent) throws InterruptedException {
        currentlyHeldWriteLock = acquireWriteLock();
        try {
            this.parent = parent;
            // We don't update the graph here because it will happen during the add process.
        } finally {
            releaseWriteLock();
        }
    }

    @Override
    @Nullable
    public NodeParent getParent() throws InterruptedException {
        currentlyHeldReadLock = acquireReadLock();
        try {
            return parent;
        } finally {
            releaseWriteLock();
        }
    }

    @RequiresWriteLock
    @Override
    public void modelMatrixUpdated() {

    }

    @Override
    public int intersectBounds(@NonNull AABB bounds) {
        //TODO: Implement intersection, while accounting for visibility of parent nodes and leaf objects...
        return Intersector.INTERSECT;
    }

    @Override
    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    @Override
    public boolean isVisible() {
        return visible;
    }

    public List<RenderableObject> getVisibleObjects() {
        if (isVisible()) {
            // TODO recursive accumulation for this node
            return null;
        }
        return null;
    }

    @RequiresWriteLock
    public void updateGraph() {
        final Matrix4 parentWorldModelMatrix = new Matrix4();
        // Walk up the graph to get the world space model matrix
        if (parent != null) {
            parent.setToModelMatrix(parentWorldModelMatrix);
        }
        // Recursively recalculate our model matrices so that the graph update does not need to do any extra work
        recalculateModelMatrix(parentWorldModelMatrix);
        // Recursively recalculate our bounds so that the graph update does not need to do any extra work
        recalculateBounds(true);
        if (parent != null) {
            // Propagate the call up the chain of parents until we reach the graph
            parent.updateGraph();
        }
    }

    @RequiresReadLock
    @Override
    public void setToModelMatrix(@NonNull Matrix4 matrix) {
        matrix.leftMultiply(getTransformation().getLocalModelMatrix());
        if (parent != null) {
            parent.setToModelMatrix(matrix);
        }
    }

    @NonNull
    @Override
    public Matrix4 getWorldModelMatrix() {
        return getTransformation().getWorldModelMatrix();
    }

    /**
     * Causes a recalculation of the min/max coordinates in local coordinate space. Requires that the local and world
     * model matrices be valid.
     *
     * @param recursive If {@code boolean}, the calculation will be made recursively across all children. If {@code
     *                  false} the child bounds will be assumed to be unchanged.
     */
    @RequiresWriteLock
    @Override
    public void recalculateBounds(boolean recursive) {
        SceneNode child;
        NodeMember member;

        final Matrix4 worldModelMatrix = getWorldModelMatrix();

        if (members.size() > 0) {
            // Pick the first member to get some valid start value for the bounds
            member = members.get(0);
            minBound.setAll(member.getMinBound()).multiply(worldModelMatrix);
            maxBound.setAll(member.getMinBound()).multiply(worldModelMatrix);
        } else if (children.size() > 0) {
            // Pick the first child to get some valid start value for the bounds
            child = children.get(0);
            minBound.setAll(child.getMinBound());
            maxBound.setAll(child.getMaxBound());
        } else {
            // Reset the bounds to 0 since this node has nothing
            minBound.setAll(0d, 0d, 0d);
            maxBound.setAll(0d, 0d, 0d);
        }
        // For each child node, recalculate the bounds as if it was an addition one at a time.
        for (int i = 0, j = children.size(); i < j; ++i) {
            child = children.get(i);
            if (recursive) {
                // Recursively check all the children
                recalculateBoundsForAdd(child);
            } else {
                // Assume the child bounds are valid
                AABB.Comparator.checkAndAdjustMinBounds(minBound, child.getMinBound());
                AABB.Comparator.checkAndAdjustMaxBounds(maxBound, child.getMaxBound());
            }
        }
        // For each child member, recalculate the bounds as if it was an addition one at a time.
        for (int i = 0, j = members.size(); i < j; ++i) {
            member = members.get(i);
            if (recursive) {
                // Recursively check all the children
                recalculateBoundsForAdd(member);
            } else {
                // Assume the child bounds are valid
                AABB.Comparator.checkAndAdjustMinBounds(minBound, member.getMinBound().clone().multiply(worldModelMatrix));
                AABB.Comparator.checkAndAdjustMaxBounds(maxBound, member.getMaxBound().clone().multiply(worldModelMatrix));
            }
        }
    }

    @RequiresWriteLock
    @Override
    public void recalculateBoundsForAdd(@NonNull SceneNode added) {
        // Have the added node determine its bounds
        added.recalculateBounds(true);
        AABB.Comparator.checkAndAdjustMinBounds(getMinBound(), added.getMinBound());
        AABB.Comparator.checkAndAdjustMaxBounds(getMaxBound(), added.getMaxBound());
    }

    /**
     * Adds a {@link NodeMember} to this {@link NodeParent}. If this parent (and any parents) are not yet part of a
     * {@link SceneGraph} no locking will be necessary and the operation will complete immediately.
     *
     * @param member The {@link NodeMember} to add.
     *
     * @throws InterruptedException Thrown if the calling thread was interrupted while waiting for lock acquisition.
     */
    public void addNodeMember(@NonNull NodeMember member) throws InterruptedException {
        acquireWriteLock();
        try {
            members.add(member);
            member.setParent(this);
            updateGraph();
        } finally {
            releaseWriteLock();
        }
    }

    /**
     * Removes a member {@link NodeMember} from this {@link NodeParent}. If this parent (and any parents) are not yet
     * part of a {@link SceneGraph} no locking will be necessary and the operation will complete immediately.
     *
     * @param member The {@link NodeMember} to remove.
     *
     * @return {@code true} if the data structure was modified by this operation.
     *
     * @throws InterruptedException Thrown if the calling thread was interrupted while waiting for lock acquisition.
     */
    public boolean removeNodeMember(@NonNull NodeMember member) throws InterruptedException {
        acquireWriteLock();
        boolean removed;
        try {
            removed = members.remove(member);
            member.setParent(null);
            updateGraph();
        } finally {
            releaseWriteLock();
        }
        return removed;
    }

    /**
     * Adds a child {@link SceneNode} to this {@link SceneNode}. If this node (and any parents) are not yet part of a
     * {@link SceneGraph} no locking will be necessary and the operation will complete immediately.
     *
     * @param node The child {@link SceneNode} to add.
     *
     * @throws InterruptedException Thrown if the calling thread was interrupted while waiting for lock acquisition.
     */
    public void addChildNode(@NonNull SceneNode node) throws InterruptedException {
        acquireWriteLock();
        try {
            children.add(node);
            node.setParent(this);
            updateGraph();
        } finally {
            releaseWriteLock();
        }
    }

    /**
     * Removes a child {@link SceneNode} from this {@link SceneNode}. If this node (and any parents) are not yet part
     * of a {@link SceneGraph} no locking will be necessary and the operation will complete immediately.
     *
     * @param node The child {@link SceneNode} to remove.
     *
     * @return {@code true} if the data structure was modified by this operation.
     *
     * @throws InterruptedException Thrown if the calling thread was interrupted while waiting for lock acquisition.
     */
    public boolean removeChildNode(@NonNull SceneNode node) throws InterruptedException {
        acquireWriteLock();
        boolean removed = false;
        try {
            removed = children.remove(node);
            node.setParent(null);
            updateGraph();
        } finally {
            releaseWriteLock();
        }
        return removed;
    }

    /**
     * Causes a recalculation of the min/max coordinates in local coordinate space, optimized for the case of a single
     * node member being added.
     *
     * @param added {@link NodeMember} implementation which was added.
     */
    @RequiresWriteLock
    protected void recalculateBoundsForAdd(@NonNull NodeMember added) {
        // Have the added node or child determine its bounds
        added.recalculateBounds();
        final Matrix4 localModelMatrix = getTransformation().getLocalModelMatrix();
        final Vector3 min = added.getMinBound().clone();
        final Vector3 max = added.getMaxBound().clone();
        AABB.Comparator.checkAndAdjustMinBounds(minBound, min.multiply(localModelMatrix));
        AABB.Comparator.checkAndAdjustMaxBounds(maxBound, max.multiply(localModelMatrix));
    }

    /**
     * Traverses the scene graph and causes all {@link NodeParent}s which implement transformations to recalculate
     * their local model matrices, and also instruct their children to do the same. This is called after scene graph
     * updates or updates to an individual node to ensure that all model matrices in the tree are kept up to date
     * after modifications.
     *
     * @param parentWorldMatrix {@link Matrix4} The parent world space model matrix. If no parent exists, this should
     *                                         be an identity matrix.
     */
    @RequiresWriteLock
    protected void recalculateModelMatrix(@NonNull Matrix4 parentWorldMatrix) {
        final Transformation localTransformation = getTransformation();
        // Recalculate the local and world model matrices
        localTransformation.calculateLocalModelMatrix();
        localTransformation.calculateWorldModelMatrix(parentWorldMatrix);
        // Take any actions needed for matrix update
        modelMatrixUpdated();
        // For each child member, notify that the model matrix has been updated.
        for (int i = 0, j = members.size(); i < j; ++i) {
            members.get(i).modelMatrixUpdated();
        }
        // For each child node, recalculate the model matrix.
        for (int i = 0, j = children.size(); i < j; ++i) {
            children.get(i).recalculateModelMatrix(localTransformation.getWorldModelMatrix());
        }
    }

    /**
     * Releases any write lock this node might be holding.
     */
    protected void releaseWriteLock() {
        // We do this from the lock directly because our parent may have become null and we need to be sure we
        // release the lock we hold.
        if (currentlyHeldWriteLock != null) {
            try {
                currentlyHeldWriteLock.unlock();
            } catch (IllegalMonitorStateException e) {
                // We did not hold a valid lock here
                currentlyHeldWriteLock = null;
            }
        }
    }

    /**
     * Releases any read lock this node might be holding.
     */
    protected void releaseReadLock() {
        // We do this from the lock directly because our parent may have become null and we need to be sure we
        // release the lock we hold.
        if (currentlyHeldReadLock != null) {
            try {
                currentlyHeldReadLock.unlock();
            } catch (IllegalMonitorStateException e) {
                // We did not hold a valid lock here
                currentlyHeldReadLock = null;
            }
        }
    }
}
