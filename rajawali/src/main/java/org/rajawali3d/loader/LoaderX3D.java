package org.rajawali3d.loader;

import android.content.res.Resources;
import android.graphics.Color;

import org.rajawali3d.Object3D;
import org.rajawali3d.animation.Animation;
import org.rajawali3d.animation.Animation3D;
import org.rajawali3d.animation.SplineOrientationAnimation3D;
import org.rajawali3d.cameras.Camera;
import org.rajawali3d.curves.Path4D;
import org.rajawali3d.lights.ALight;
import org.rajawali3d.lights.DirectionalLight;
import org.rajawali3d.lights.PointLight;
import org.rajawali3d.lights.SpotLight;
import org.rajawali3d.loader.AMeshLoader;
import org.rajawali3d.loader.ParsingException;
import org.rajawali3d.materials.Material;
import org.rajawali3d.materials.methods.DiffuseMethod;
import org.rajawali3d.materials.textures.TextureManager;
import org.rajawali3d.math.Quaternion;
import org.rajawali3d.math.vector.Vector3;
import org.rajawali3d.primitives.Cylinder;
import org.rajawali3d.primitives.NPrism;
import org.rajawali3d.primitives.Plane;
import org.rajawali3d.primitives.RectangularPrism;
import org.rajawali3d.primitives.Sphere;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.InputStream;
import java.util.List;
import java.util.Stack;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

public class LoaderX3D extends AMeshLoader {
    XPathFactory xPathfactory;
    Document mDoc;

    public LoaderX3D(Resources resources, TextureManager textureManager, int resourceId) {
        super(resources, textureManager, resourceId);
        xPathfactory = XPathFactory.newInstance();
    }

    public AMeshLoader parse() throws ParsingException {
        super.parse();
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            InputStream is = mResources.openRawResource(mResourceId);
            mDoc = builder.parse(is);

            Element scene = (Element) mDoc.getDocumentElement().getElementsByTagName("Scene").item(0);
            NodeList children = scene.getChildNodes();
            for(int i=0; i<children.getLength(); i++) {
                if(children.item(i).getNodeName().equals("Transform")) {
                    parseTransform(mRootObject, (Element) children.item(i));
                }
                if(children.item(i).getNodeName().equals("Shape")) {
                    parseShape(mRootObject, (Element) children.item(i));
                }
            }
        } catch (Exception e) {
            throw new ParsingException(e.getMessage());
        }
        return this;
    }

    public List<ALight> getParsedLights() {
        Stack<ALight> lights = new Stack<>();
        Element scene = (Element) mDoc.getDocumentElement().getElementsByTagName("Scene").item(0);

        NodeList directionalLights = scene.getElementsByTagName("DirectionalLight");
        for(int i=0; i<directionalLights.getLength(); i++) {
            Node tag;
            tag = directionalLights.item(i).getAttributes().getNamedItem("intensity");
            float intensity = tag==null ? 1 : Float.parseFloat(tag.getNodeValue());
            tag = directionalLights.item(i).getAttributes().getNamedItem("color");
            float[] rgb = { 1,1,1 };
            if(tag!=null) {
                String[] colorChannels = tag.getNodeValue().split("\\s+");
                rgb[0] = Float.parseFloat(colorChannels[0]);
                rgb[1] = Float.parseFloat(colorChannels[1]);
                rgb[2] = Float.parseFloat(colorChannels[2]);
            }
            tag = directionalLights.item(i).getAttributes().getNamedItem("location");
            Vector3 location = new Vector3();
            if(tag!=null) {
                String[] axes = tag.getNodeValue().split("\\s+");
                location.x = Float.parseFloat(axes[0]);
                location.y = Float.parseFloat(axes[1]);
                location.z = Float.parseFloat(axes[2]);
            }
            tag = directionalLights.item(i).getAttributes().getNamedItem("direction");
            Vector3 direction = new Vector3();
            if(tag!=null) {
                String[] axes = tag.getNodeValue().split("\\s+");
                direction.x = Float.parseFloat(axes[0]);
                direction.y = Float.parseFloat(axes[1]);
                direction.z = Float.parseFloat(axes[2]);
            }

            DirectionalLight directionalLight = new DirectionalLight();
            directionalLight.setPower(intensity);
            directionalLight.setLookAt(location.add(direction));
            directionalLight.setColor(rgb[0], rgb[1], rgb[2]);
            directionalLight.setPosition(location);
            lights.add(directionalLight);
        }

        NodeList pointLights = scene.getElementsByTagName("PointLight");
        for(int i=0; i<pointLights.getLength(); i++) {
            Node tag;
            tag = pointLights.item(i).getAttributes().getNamedItem("intensity");
            float intensity = tag==null ? 1 : Float.parseFloat(tag.getNodeValue());
            tag = pointLights.item(i).getAttributes().getNamedItem("color");
            float[] rgb = { 1,1,1 };
            if(tag!=null) {
                String[] colorChannels = tag.getNodeValue().split("\\s+");
                rgb[0] = Float.parseFloat(colorChannels[0]);
                rgb[1] = Float.parseFloat(colorChannels[1]);
                rgb[2] = Float.parseFloat(colorChannels[2]);
            }
            tag = directionalLights.item(i).getAttributes().getNamedItem("location");
            Vector3 location = new Vector3();
            if(tag!=null) {
                String[] axes = tag.getNodeValue().split("\\s+");
                location.x = Float.parseFloat(axes[0]);
                location.y = Float.parseFloat(axes[1]);
                location.z = Float.parseFloat(axes[2]);
            }

            PointLight pointLight = new PointLight();
            pointLight.setPower(intensity);
            pointLight.setColor(rgb[0], rgb[1], rgb[2]);
            pointLight.setPosition(location);
            lights.add(pointLight);
        }

        NodeList spotLights = scene.getElementsByTagName("SpotLight");
        for(int i=0; i<spotLights.getLength(); i++) {
            Node tag;
            tag = spotLights.item(i).getAttributes().getNamedItem("intensity");
            float intensity = tag==null ? 1 : Float.parseFloat(tag.getNodeValue());
            tag = spotLights.item(i).getAttributes().getNamedItem("color");
            float[] rgb = { 1,1,1 };
            if(tag!=null) {
                String[] colorChannels = tag.getNodeValue().split("\\s+");
                rgb[0] = Float.parseFloat(colorChannels[0]);
                rgb[1] = Float.parseFloat(colorChannels[1]);
                rgb[2] = Float.parseFloat(colorChannels[2]);
            }
            tag = directionalLights.item(i).getAttributes().getNamedItem("location");
            Vector3 location = new Vector3();
            if(tag!=null) {
                String[] axes = tag.getNodeValue().split("\\s+");
                location.x = Float.parseFloat(axes[0]);
                location.y = Float.parseFloat(axes[1]);
                location.z = Float.parseFloat(axes[2]);
            }

            SpotLight spotlight = new SpotLight();
            spotlight.setPower(intensity);
            spotlight.setColor(rgb[0], rgb[1], rgb[2]);
            spotlight.setPosition(location);
            lights.add(spotlight);
        }
        return lights;
    }

    public Camera getParsedCamera() {
        String value;
        Camera camera = new Camera();
        camera.setUpAxis(Vector3.Axis.Z);

        Element viewpoint = (Element) mDoc.getDocumentElement().getElementsByTagName("Viewpoint").item(0);
        if(viewpoint == null) return null;

        value = viewpoint.getAttribute("position").trim();
        if(value.length()>0) {
            String[] translation = value.split("\\s+");
            camera.setPosition(new Vector3(
                    Float.parseFloat(translation[0]),
                    Float.parseFloat(translation[1]),
                    Float.parseFloat(translation[2])
            ));
        }

        value = viewpoint.getAttribute("orientation").trim();
        if(value.length()>0) {
            String[] rotation = value.split("\\s+");
            Vector3 axis = new Vector3(
                    Double.parseDouble(rotation[0]),
                    Double.parseDouble(rotation[1]),
                    Double.parseDouble(rotation[2])
            );
            Double angle = Math.toDegrees(Double.parseDouble(rotation[3]));
            Quaternion q = new Quaternion(axis,-angle);
            q.normalize();
            camera.setOrientation(q);
        }
        return camera;
    }

    public List<Animation3D> getParsedAnimations() throws XPathExpressionException {
        Stack<Animation3D> animations = new Stack<>();
        Element scene = (Element) mDoc.getDocumentElement().getElementsByTagName("Scene").item(0);
        XPath xpath = xPathfactory.newXPath();

        NodeList orientationInterpolator = scene.getElementsByTagName("OrientationInterpolator");
        for(int i=0; i<orientationInterpolator.getLength(); i++) {
            Node tag;
            Node node;
            NodeList nodes;
            XPathExpression expr;

            tag = orientationInterpolator.item(i).getAttributes().getNamedItem("DEF");
            String id = tag.getNodeValue();
            tag = orientationInterpolator.item(i).getAttributes().getNamedItem("keyValue");
            Path4D path = parseAxisAngleTo4D(tag.getNodeValue().trim());

            expr = xpath.compile("//ROUTE[@fromNode=\"" + id + "\"]");
            nodes = (NodeList) expr.evaluate(scene, XPathConstants.NODESET);
            node = nodes.item(0);
            String target = node.getAttributes().getNamedItem("toNode").getNodeValue().trim();

            expr = xpath.compile("//ROUTE[@toNode=\"" + id + "\"]");
            nodes = (NodeList) expr.evaluate(scene, XPathConstants.NODESET);
            node = nodes.item(0);
            String source = node.getAttributes().getNamedItem("fromNode").getNodeValue().trim();

            expr = xpath.compile("//TimeSensor[@DEF=\"" + source + "\"]");
            nodes = (NodeList) expr.evaluate(scene, XPathConstants.NODESET);
            node = nodes.item(0);
            double cycleInterval = Double.parseDouble(node.getAttributes().getNamedItem("cycleInterval").getNodeValue().trim());
            boolean loop = Boolean.parseBoolean(node.getAttributes().getNamedItem("loop").getNodeValue().trim());

            Animation3D anim = new SplineOrientationAnimation3D(path);
            anim.setDurationDelta(cycleInterval);
            anim.setTransformable3D(mRootObject.getChildByName(target));
            if(loop) anim.setRepeatMode(Animation.RepeatMode.INFINITE);
            animations.add(anim);
        }
        return animations;
    }

    Path4D parseAxisAngleTo4D(String keyValue) {
        Path4D path = new Path4D();
        String[] values = keyValue.split("\\s+");
        for(int i=0; i<values.length; i+=4) {
            Vector3 axis = new Vector3(
                    Double.parseDouble(values[i+0]),
                    Double.parseDouble(values[i+1]),
                    Double.parseDouble(values[i+2])
            );
            Double angle = Math.toDegrees(Double.parseDouble(values[i+3]));
            Quaternion point = new Quaternion(axis,-angle);
            point.normalize();
            path.addPoint(point);
        }
        return path;
    }

    static void parseTransform(Object3D parent, Element transform) {
        String value;
        Object3D object = new Object3D();

        value = transform.getAttribute("DEF").trim();
        if(value.length()>0) {
            object.setName(value);
        }

        value = transform.getAttribute("scale").trim();
        if(value.length()>0) {
            String[] scale = value.split("\\s+");
            object.setScale(new Vector3(
                    Float.parseFloat(scale[0]),
                    Float.parseFloat(scale[1]),
                    Float.parseFloat(scale[2])
            ));
        }

        value = transform.getAttribute("rotation").trim();
        if(value.length()>0) {
            String[] rotation = value.split("\\s+");
            Quaternion q = new Quaternion(
                    Float.parseFloat(rotation[0]),
                    Float.parseFloat(rotation[1]),
                    Float.parseFloat(rotation[2]),
                    Float.parseFloat(rotation[2])
            );
            q.normalize();
            object.rotate(q);
        }

        value = transform.getAttribute("translation").trim();
        if(value.length()>0) {
            String[] translation = value.split("\\s+");
            object.setPosition(new Vector3(
                    Float.parseFloat(translation[0]),
                    Float.parseFloat(translation[1]),
                    Float.parseFloat(translation[2])
            ));
        }
        parent.addChild(object);

        NodeList children = transform.getChildNodes();
        for(int i=0; i<children.getLength(); i++) {
            if(children.item(i).getNodeName().equals("Transform")) {
                parseTransform(object, (Element) children.item(i));
            }
            if(children.item(i).getNodeName().equals("Shape")) {
                parseShape(object, (Element) children.item(i));
            }
        }
    }

    static void parseShape(Object3D parent, Element shape) {
        genPrimitive(parent, genMaterial(shape), shape);
    }

    static Material genMaterial(Element shape) {
        Material material = new Material();
        Node diffuseColor = null;
        Node mat = shape.getElementsByTagName("Material").item(0);
        if(mat != null) {
            diffuseColor = mat.getAttributes().getNamedItem("diffuseColor");
        }

        if(diffuseColor == null) {
            material.setColor(Color.WHITE);
        } else {
            String[] colorChannels = diffuseColor.getNodeValue().split("\\s+");
            float[] rgba = {
                    Float.parseFloat(colorChannels[0]),
                    Float.parseFloat(colorChannels[1]),
                    Float.parseFloat(colorChannels[2]),
                    1
            };
            material.setColor(rgba);
            material.setDiffuseMethod(new DiffuseMethod.Lambert());
            material.enableLighting(true);
        }
        return material;
    }

    static void genPrimitive(Object3D parent, Material material, Element shape) {
        Node box = shape.getElementsByTagName("Box").item(0);
        if(box != null) {
            Node tag;
            tag = box.getAttributes().getNamedItem("size");
            if(tag==null) {
                RectangularPrism rect = new RectangularPrism(1,1,1);
                rect.setMaterial(material);
                parent.addChild(rect);
            } else {
                String[] dimensions = tag.getNodeValue().split("\\s+");
                float width  = Float.parseFloat(dimensions[0]);
                float height = Float.parseFloat(dimensions[1]);
                float depth  = Float.parseFloat(dimensions[2]);
                RectangularPrism obj = new RectangularPrism(width,height,depth);
                obj.setMaterial(material);
                parent.addChild(obj);
            }
        }

        Node cone = shape.getElementsByTagName("Cone").item(0);
        if(cone != null) {
            Node tag;
            tag = cone.getAttributes().getNamedItem("height");
            float height = tag==null ? 1 : Float.parseFloat(tag.getNodeValue());
            tag = cone.getAttributes().getNamedItem("bottomRadius");
            float radiusBottom = tag==null ? 1 : Float.parseFloat(tag.getNodeValue());
            NPrism obj = new NPrism(16,0,radiusBottom,height);
            obj.setMaterial(material);
            parent.addChild(obj);
        }

        Node cylinder = shape.getElementsByTagName("Cylinder").item(0);
        if(cylinder != null) {
            Node tag;
            tag = cylinder.getAttributes().getNamedItem("height");
            float length = tag==null ? 1 : Float.parseFloat(tag.getNodeValue());
            tag = cylinder.getAttributes().getNamedItem("radius");
            float radius = tag==null ? 1 : Float.parseFloat(tag.getNodeValue());
            Cylinder obj = new Cylinder(length,radius,1,16);
            obj.setMaterial(material);
            parent.addChild(obj);
        }

        Node sphere = shape.getElementsByTagName("Sphere").item(0);
        if(sphere != null) {
            Node tag;
            tag = sphere.getAttributes().getNamedItem("radius");
            float radius = tag==null ? 1 : Float.parseFloat(tag.getNodeValue());
            Sphere obj = new Sphere(radius,32,16);
            obj.setMaterial(material);
            parent.addChild(obj);
        }

        Node plane = shape.getElementsByTagName("Plane").item(0);
        if(plane != null) {
            Node tag;
            tag = plane.getAttributes().getNamedItem("DEF");
            String name = tag==null ? null : tag.getNodeValue().trim();
            tag = plane.getAttributes().getNamedItem("size");
            String[] dimensions = tag.getNodeValue().split("\\s+");
            float width  = Float.parseFloat(dimensions[0]);
            float height = Float.parseFloat(dimensions[1]);
            Plane obj = new Plane(width, height, 1,1);
            obj.setMaterial(material);
            parent.addChild(obj);
        }
    }
}
