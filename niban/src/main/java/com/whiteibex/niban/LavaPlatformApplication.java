package com.whiteibex.niban;

import java.io.File;
import java.util.List;

import com.jme3.app.BasicProfilerState;
import com.jme3.app.DebugKeysAppState;
import com.jme3.app.FlyCamAppState;
import com.jme3.app.SimpleApplication;
import com.jme3.app.StatsAppState;
import com.jme3.asset.BlenderKey;
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.material.TechniqueDef.LightMode;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.post.FilterPostProcessor;
import com.jme3.post.filters.BloomFilter;
import com.jme3.post.filters.BloomFilter.GlowMode;
import com.jme3.post.filters.GammaCorrectionFilter;
import com.jme3.post.ssao.SSAOFilter;
import com.jme3.scene.LightNode;
import com.jme3.scene.Node;
import com.jme3.system.NativeLibraryLoader;

public class LavaPlatformApplication extends SimpleApplication {

  public static void main(String[] args) {
    File extractionFolder = new File(System.getProperty("user.home"), ".niban/natives/");
    extractionFolder.mkdirs();
    NativeLibraryLoader.setCustomExtractionFolder(extractionFolder.getAbsolutePath());

    new LavaPlatformApplication().start();
  }

  public LavaPlatformApplication() {
    super(new StatsAppState(), new FlyCamAppState(), new DebugKeysAppState(), new BasicProfilerState());
  }

  @Override
  public void simpleInitApp() {
    renderManager.setPreferredLightMode(LightMode.SinglePass);
    renderManager.setSinglePassLightBatchSize(5);

    BlenderKey blenderKey = new BlenderKey("scene/lava-platform.blend");
    blenderKey.setLayersToLoad(1);
    blenderKey.excludeFromLoading(BlenderKey.FeaturesToLoad.TEXTURES | BlenderKey.FeaturesToLoad.CAMERAS);
    Node scene = (Node) assetManager.loadModel(blenderKey);

    ColorRGBA lightColor = null;
    List<LightNode> lightNodes = scene.descendantMatches(LightNode.class, "LavaDirect");
    for (int i = 0; i < lightNodes.size(); i++) {
      LightNode lightNode = lightNodes.get(i);

      lightNode.getParent().detachChild(lightNode);
      scene.removeLight(lightNode.getLight());

      DirectionalLight light = new DirectionalLight();
      light.setColor(lightNode.getLight().getColor());
      light.setDirection(lightNode.getWorldRotation().mult(Vector3f.UNIT_Y).mult(-1));
      scene.addLight(light);

      lightColor = light.getColor();
      light.getColor().multLocal(0.1f + (lightNodes.size() - i) * 0.3f);
    }

    rootNode.attachChild(scene);

    AmbientLight ambientLight = new AmbientLight();
    ambientLight.setColor(lightColor.mult(0.2f));
    rootNode.addLight(ambientLight);

    FilterPostProcessor filterPostProcessor = new FilterPostProcessor(assetManager);
    filterPostProcessor.setNumSamples(Math.max(1, getContext().getSettings().getSamples()));

    SSAOFilter ssaoFilter = new SSAOFilter();
    ssaoFilter.setSampleRadius(1);
    filterPostProcessor.addFilter(ssaoFilter);

    BloomFilter bloomFilter = new BloomFilter(GlowMode.Scene);
    bloomFilter.setBloomIntensity(1);
    bloomFilter.setDownSamplingFactor(2);
    filterPostProcessor.addFilter(bloomFilter);

    GammaCorrectionFilter gammaCorrectionFilter = new GammaCorrectionFilter();
    filterPostProcessor.addFilter(gammaCorrectionFilter);

    viewPort.addProcessor(filterPostProcessor);
  }
}
