package com.whiteibex.niban;

import java.io.File;
import java.io.IOException;

import com.jme3.app.SimpleApplication;
import com.jme3.asset.BlenderKey;
import com.jme3.collision.CollisionResult;
import com.jme3.collision.CollisionResults;
import com.jme3.effect.ParticleEmitter;
import com.jme3.effect.ParticleMesh.Type;
import com.jme3.export.xml.XMLExporter;
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.light.PointLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Ray;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.post.FilterPostProcessor;
import com.jme3.post.filters.DepthOfFieldFilter;
import com.jme3.post.filters.TranslucentBucketFilter;
import com.jme3.post.ssao.SSAOFilter;
import com.jme3.renderer.queue.RenderQueue.Bucket;
import com.jme3.renderer.queue.RenderQueue.ShadowMode;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.shadow.CompareMode;
import com.jme3.shadow.DirectionalLightShadowFilter;
import com.jme3.shadow.EdgeFilteringMode;
import com.jme3.system.NativeLibraryLoader;
import com.jme3.texture.Texture;

public class NibanApplication extends SimpleApplication {
  private final static int SHADOWMAP_SIZE = 2048;

  public static void main(String[] args) {
    File extractionFolder = new File(System.getProperty("user.home"), ".niban/natives/");
    extractionFolder.mkdirs();
    NativeLibraryLoader.setCustomExtractionFolder(extractionFolder.getAbsolutePath());

    NibanApplication nibanApplication = new NibanApplication();
    nibanApplication.start();
  }

  private FilterPostProcessor filterPostProcessor;

  @Override
  public void simpleInitApp() {
    System.out.println(settings);

    setDisplayStatView(true);
    setDisplayFps(true);
    cam.setFrustumFar(200);
    flyCam.setMoveSpeed(7);
    viewPort.setBackgroundColor(ColorRGBA.Gray);

    BlenderKey blenderKey = new BlenderKey("scene/tree.jme.blend");
    blenderKey.setLayersToLoad(1);
    blenderKey.excludeFromLoading(BlenderKey.FeaturesToLoad.LIGHTS
        | BlenderKey.FeaturesToLoad.TEXTURES
        | BlenderKey.FeaturesToLoad.CAMERAS);
    Node scene = (Node) assetManager.loadModel(blenderKey);

    // Shadow modes
    scene.setShadowMode(ShadowMode.CastAndReceive);
    scene.getChild("GroundG1").setShadowMode(ShadowMode.Receive);
    scene.getChild("FireG1").setShadowMode(ShadowMode.Off);
    Vector3f fireTranslation = scene.getChild("Fire").getWorldTranslation();

    Material material = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
    material.setColor("Diffuse", ColorRGBA.LightGray);
    material.setColor("Ambient", ColorRGBA.DarkGray);
    material.setBoolean("VertexLighting", false);
    // scene.setMaterial(material);

    // makeToonish(scene);

    rootNode.attachChild(scene);

    // skylight
    DirectionalLight directionalLight = new DirectionalLight();
    directionalLight.setDirection(new Vector3f(-0.6f, -1, -0.9f).normalizeLocal());
    directionalLight.setColor(ColorRGBA.Blue.add(ColorRGBA.LightGray));
    rootNode.addLight(directionalLight);

    AmbientLight ambientLight = new AmbientLight();
    ambientLight.setColor(ColorRGBA.Gray);
    rootNode.addLight(ambientLight);

    PointLight pointLight = new PointLight();
    pointLight.setColor(ColorRGBA.Orange.mult(7));
    pointLight.setRadius(15);
    pointLight.setPosition(fireTranslation.add(0, .2f, 0));
    rootNode.addLight(pointLight);

    ParticleEmitter particleEmitter = new ParticleEmitter("Emitter", Type.Triangle, 100);
    particleEmitter.setGravity(0, 0, 0);
    particleEmitter.getParticleInfluencer().setInitialVelocity(Vector3f.UNIT_Y);
    particleEmitter.setLowLife(5);
    particleEmitter.setHighLife(15);
    particleEmitter.setImagesX(15);
    Material particleEmitterMaterial = new Material(assetManager, "Common/MatDefs/Misc/Particle.j3md");
    particleEmitter.setMaterial(particleEmitterMaterial);
    particleEmitter.setQueueBucket(Bucket.Translucent);
    particleEmitter.setLocalTranslation(fireTranslation);
    rootNode.attachChild(particleEmitter);

    initializeFilters(directionalLight);
  }

  private void makeToonish(Spatial spatial) {
    if (spatial instanceof Node) {
      Node node = (Node) spatial;
      for (Spatial child : node.getChildren()) {
        makeToonish(child);
      }
    } else if (spatial instanceof Geometry) {
      Geometry geometry = (Geometry) spatial;
      Material material = geometry.getMaterial();
      if (material.getMaterialDef().getName().equals("Phong Lighting")) {
        Texture colorRampTexture = assetManager.loadTexture("Textures/ColorRamp/toon.png");
        colorRampTexture.setMinFilter(Texture.MinFilter.NearestNoMipMaps);
        colorRampTexture.setMagFilter(Texture.MagFilter.Nearest);
        material.setTexture("ColorRamp", colorRampTexture);
      }
    }
  }

  private void initializeFilters(DirectionalLight directionalLight) {
    DirectionalLightShadowFilter shadowFilter = new DirectionalLightShadowFilter(assetManager, SHADOWMAP_SIZE, 3);
    shadowFilter.setLight(directionalLight);
    shadowFilter.setShadowCompareMode(CompareMode.Software);
    shadowFilter.setEdgeFilteringMode(EdgeFilteringMode.Dither);
    shadowFilter.setShadowIntensity(0.4f);
    shadowFilter.setShadowZExtend(90);
    shadowFilter.setShadowZFadeLength(10);

    SSAOFilter ssaoFilter = new SSAOFilter();
    ssaoFilter.setSampleRadius(2);
    ssaoFilter.setIntensity(2);
    ssaoFilter.setBias(0.2f);

    HeterogenousFogFilter fogFilter = new HeterogenousFogFilter();
    fogFilter.setFogColor(ColorRGBA.LightGray);
    fogFilter.setFogDistance(50);
    fogFilter.setFogDensity(0.7f);

    TranslucentBucketFilter translucentBucketFilter = new TranslucentBucketFilter();

    DepthOfFieldFilter depthOfFieldFilter = new DepthOfFieldFilter();
    depthOfFieldFilter.setFocusRange(100);

    filterPostProcessor = new FilterPostProcessor(assetManager);
    filterPostProcessor.addFilter(shadowFilter);
    // filterPostProcessor.addFilter(ssaoFilter);
    filterPostProcessor.addFilter(fogFilter);
    filterPostProcessor.addFilter(translucentBucketFilter);
    // filterPostProcessor.addFilter(depthOfFieldFilter);
    viewPort.addProcessor(filterPostProcessor);

    try {
      System.out.println(XMLExporter.getInstance().save(filterPostProcessor, System.out));
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }

  private final Vector3f camDirection = new Vector3f();
  private final float[] camDistances = new float[5];

  @Override
  public void simpleUpdate(float tpf) {
    cam.getDirection(camDirection);

    float offset = getContext().getSettings().getWidth() * 0.2f;
    camDistances[0] = getDofDistance(0, 0);
    camDistances[1] = getDofDistance(offset, 0);
    camDistances[2] = getDofDistance(-offset, 0);
    camDistances[3] = getDofDistance(0, offset);
    camDistances[4] = getDofDistance(0, -offset);

    float minDistance = cam.getFrustumFar();
    float maxDistance = 0;
    for (float d : camDistances) {
      minDistance = Math.min(minDistance, d);
      maxDistance = Math.max(maxDistance, d);
    }

    DepthOfFieldFilter dofFilter = filterPostProcessor.getFilter(DepthOfFieldFilter.class);
    if (dofFilter != null) {
      dofFilter.setFocusDistance((camDistances[0] + camDistances[1] + camDistances[2] + camDistances[3] + camDistances[4]) / 4.0f);
      dofFilter.setFocusRange((maxDistance - minDistance) * 10.0f);
      System.out.println(dofFilter.getFocusDistance() + " / " + dofFilter.getFocusRange());
    }
  }

  private float getDofDistance(float xOffset, float yOffset) {
    Vector3f location = cam.getWorldCoordinates(new Vector2f(settings.getWidth() / 2 + xOffset, settings.getHeight() / 2 + yOffset), 0.0f);

    Ray ray = new Ray(location, camDirection);
    CollisionResults results = new CollisionResults();
    int numCollisions = rootNode.collideWith(ray, results);
    float distance = cam.getFrustumFar();
    if (numCollisions > 0) {
      CollisionResult hit = results.getClosestCollision();
      distance = hit.getDistance();
    }
    return distance;
  }
}
