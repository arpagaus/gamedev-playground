package com.whiteibex.niban;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;

import com.jme3.asset.AssetManager;
import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.post.filters.FogFilter;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.terrain.noise.basis.ImprovedNoise;
import com.jme3.texture.Image;
import com.jme3.texture.Image.Format;
import com.jme3.texture.Texture.MagFilter;
import com.jme3.texture.Texture.WrapAxis;
import com.jme3.texture.Texture.WrapMode;
import com.jme3.texture.Texture3D;
import com.jme3.texture.image.ColorSpace;
import com.jme3.util.BufferUtils;

/**
 * 
 * @see http://www.cescg.org/CESCG-2004/web/Zdrojewska-Dorota/
 */
public class HeterogenousFogFilter extends FogFilter {

  protected float time = 0;
  protected float speed = 1;
  protected Vector3f noiseScale = new Vector3f(1000, 200, 400);
  protected Vector3f windDirection = new Vector3f(1.0f, 0.5f, 0);

  public HeterogenousFogFilter() {
    super();
  }

  public HeterogenousFogFilter(ColorRGBA fogColor, float fogDensity, float fogDistance) {
    super(fogColor, fogDensity, fogDistance);
  }

  public float getSpeed() {
    return speed;
  }

  public void setSpeed(float speed) {
    this.speed = speed;
  }

  public Vector3f getNoiseScale() {
    return noiseScale;
  }

  public void setNoiseScale(Vector3f noiseScale) {
    this.noiseScale = noiseScale;
  }

  public Vector3f getWindDirection() {
    return windDirection;
  }

  public void setWindDirection(Vector3f windDirection) {
    this.windDirection = windDirection;
  }

  @Override
  protected void initFilter(AssetManager manager, RenderManager renderManager, ViewPort vp, int w, int h) {
    material = new Material(manager, "Common/MatDefs/Post/HeterogenousFog.j3md");
    material.setColor("FogColor", getFogColor());
    material.setFloat("FogDensity", getFogDensity());
    material.setFloat("FogDistance", getFogDistance());
    material.setTexture("NoiseTexture", getNoiseTexture());
    material.setVector3("NoiseScale", getNoiseScale());
    material.setVector3("WindDirection", getWindDirection());
  }

  @Override
  protected void preFrame(float tpf) {
    super.preFrame(tpf);
    time = time + (tpf * speed);
    material.setFloat("Time", time);
  }

  protected Texture3D getNoiseTexture() {
    final int SIZE = 256;
    final float SCALE = 8;

    float[] data = new float[SIZE * SIZE * SIZE];
    int count = 0;

    for (int z = 0; z < SIZE; z++) {
      for (int y = 0; y < SIZE; y++) {
        for (int x = 0; x < SIZE; x++) {
          float scale = SCALE;

          float value = ImprovedNoise.noise(scale * x / SIZE, scale * y / SIZE, scale * z / SIZE) / (scale / SCALE);
          scale = scale * 2;
          value = value + (ImprovedNoise.noise(scale * x / SIZE, scale * y / SIZE, scale * z / SIZE) / (scale / SCALE));
          scale = scale * 2;
          value = value + (ImprovedNoise.noise(scale * x / SIZE, scale * y / SIZE, scale * z / SIZE) / (scale / SCALE));
          scale = scale * 2;
          value = value + (ImprovedNoise.noise(scale * x / SIZE, scale * y / SIZE, scale * z / SIZE) / (scale / SCALE));

          data[count++] = value;
        }
      }
    }

    float minValue = data[0], maxValue = data[0];
    for (int i = 0; i < data.length; i++) {
      minValue = Math.min(data[i], minValue);
      maxValue = Math.max(data[i], maxValue);
    }

    ByteBuffer buffer = BufferUtils.createByteBuffer(data.length);
    for (int i = 0; i < data.length; i++) {
      buffer.put((byte) (byte) (255 * (data[i] - minValue) / (maxValue - minValue)));
    }
    buffer.rewind();

    Texture3D texture3d = new Texture3D(new Image(Format.Luminance8, SIZE, SIZE, SIZE, new ArrayList<ByteBuffer>(Collections.singleton(buffer)), null, ColorSpace.Linear));
    texture3d.setWrap(WrapAxis.R, WrapMode.MirroredRepeat);
    texture3d.setWrap(WrapAxis.S, WrapMode.MirroredRepeat);
    texture3d.setWrap(WrapAxis.T, WrapMode.MirroredRepeat);
    texture3d.setMagFilter(MagFilter.Bilinear);
    return texture3d;
  }

  @Override
  public void write(JmeExporter ex) throws IOException {
    super.write(ex);
    OutputCapsule oc = ex.getCapsule(this);
    oc.write(speed, "speed", 1);
    oc.write(noiseScale, "noiseScale", new Vector3f(1000, 200, 400));
    oc.write(windDirection, "windDirection", new Vector3f(1.0f, 0.5f, 0));
  }

  @Override
  public void read(JmeImporter im) throws IOException {
    super.read(im);
    InputCapsule ic = im.getCapsule(this);
    speed = ic.readFloat("speed", 1);
    noiseScale = (Vector3f) ic.readSavable("noiseScale", new Vector3f(1000, 200, 400));
    windDirection = (Vector3f) ic.readSavable("windDirection", new Vector3f(1.0f, 0.5f, 0));
  }
}
