package org.rajawali3d.examples.examples.optimizations;

/*public class TextureAtlasFragment extends AExampleFragment {

	@Override
    public AExampleRenderer createRenderer() {
		return new TextureAtlasRenderer(getActivity(), this);
	}

	private final class TextureAtlasRenderer extends AExampleRenderer {
		private TextureAtlas mAtlas;
		private Material mAtlasMaterial, mSphereMaterial, mCubeMaterial, mPlaneMaterial;
		private Plane mAtlasPlane, mTilePlane;
		private Cube mTileCube;
		private Sphere mTileSphere;

		public TextureAtlasRenderer(Context context, @Nullable AExampleFragment fragment) {
			super(context, fragment);
		    getCurrentScene().setBackgroundColor(0x666666);
		}

        @Override
		public void initScene() {
			//
			// -- Pack all textures in the "assets/atlas" folder into an 1024x1024 atlas
			// -- this should be used to simplify implementation of multiple textures
			// -- and to reduce texture binding calls to the GPU for increased performance
			//
			mAtlas = new TexturePacker(mContext).packTexturesFromAssets(1024, 1024, 0, false, "atlas");

			mAtlasMaterial = new Material();
			mSphereMaterial = new Material();
			mCubeMaterial = new Material();
			mPlaneMaterial = new Material();

			try {
				//
				// -- Add the entire atlas to a material so it can be shown in the example
				// -- this is not necessary in typical use cases
				//
				mAtlasMaterial.addTexture(new Texture2D("atlasTexture", mAtlas.getPages()[0]));
				mAtlasMaterial.setColorInfluence(0);
				//
				// -- Add each target texture to the material
				// -- they are pulled from the atlas by their original resource name
				//
				mSphereMaterial.addTexture(new Texture2D("earthtruecolor_nasa_big", mAtlas));
				mSphereMaterial.setColorInfluence(0);
				mCubeMaterial.addTexture(new Texture2D("camden_town_alpha", mAtlas));
				mCubeMaterial.setColorInfluence(0);
				mPlaneMaterial.addTexture(new Texture2D("rajawali", mAtlas));
				mPlaneMaterial.setColorInfluence(0);
			} catch (ATexture.TextureException e) {
				e.printStackTrace();
			}

			//
			// -- Show the full atlas for demonstration purposes
			//
			mAtlasPlane = new Plane(Vector3.Axis.Z);
			mAtlasPlane.setMaterial(mAtlasMaterial);
			mAtlasPlane.setY(1);
			getCurrentScene().addAndInitializeChild(mAtlasPlane);

			mTileSphere = new Sphere(.35f, 20, 20);
			mTileSphere.setMaterial(mAtlasMaterial);
			//
			// -- The method 'setAtlasTile' is used to scale the UVs of the target object
			// -- so that the appropriate image within the atlas is displayed
			//
			mTileSphere.setAtlasTile("earthtruecolor_nasa_big", mAtlas);
			mTileSphere.setPosition(0, -.1f, 0);
			getCurrentScene().addAndInitializeChild(mTileSphere);

			mTileCube = new Cube(.5f);
			mTileCube.setMaterial(mAtlasMaterial);
			mTileCube.setAtlasTile("camden_town_alpha", mAtlas);
			mTileCube.setPosition(-.5f, -1f, 0);
			mTileCube.setRotX(-1);
			getCurrentScene().addAndInitializeChild(mTileCube);

			mTilePlane = new Plane(.6f,.6f,1,1);
			mTilePlane.setMaterial(mAtlasMaterial);
			mTilePlane.setAtlasTile("rajawali", mAtlas);
			mTilePlane.setPosition(.5f, -1f, 0);
			getCurrentScene().addAndInitializeChild(mTilePlane);
		}

        @Override
        protected void onRender(long ellapsedRealtime, double deltaTime) {
            super.onRender(ellapsedRealtime, deltaTime);
			mTileCube.setRotY(mTileCube.getRotY()+1);
			mTileSphere.setRotY(mTileSphere.getRotY()+1);
		}
	}

}*/
