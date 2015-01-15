uniform sampler2D m_Texture;
uniform sampler2D m_DepthTexture;
varying vec2 texCoord;

uniform vec4 m_FogColor;
uniform float m_FogDensity;
uniform float m_FogDistance;

vec2 m_FrustumNearFar=vec2(1.0,m_FogDistance);
const float LOG2 = 1.442695;

uniform mat4 g_ViewProjectionMatrixInverse;
uniform sampler3D m_NoiseTexture;
uniform vec3 m_NoiseScale;
uniform float m_Time;
uniform vec3 m_WindDirection;

vec3 getPosition(in float depth, in vec2 texCoord){
  vec4 pos = vec4(texCoord, depth, 1.0) * 2.0 - 1.0;
  pos = g_ViewProjectionMatrixInverse * pos;
  return pos.xyz / pos.w;
}

void main() {
  vec4 texVal = texture2D(m_Texture, texCoord);      
  float depthVal = texture2D(m_DepthTexture, texCoord).r;
  
  float fogDepth = (2.0 * m_FrustumNearFar.x) / (m_FrustumNearFar.y + m_FrustumNearFar.x - depthVal * (m_FrustumNearFar.y - m_FrustumNearFar.x));
  
  vec3 worldPosition = getPosition(depthVal, texCoord);
  float noiseVal = texture3D(m_NoiseTexture, worldPosition / m_NoiseScale + m_Time * 0.01 * m_WindDirection).r;

  //float fogFactorVertical = clamp(1.0 / 5.0 * worldPosition.y, 0.5, 1.0);

  float fogFactor = exp2(-m_FogDensity * m_FogDensity * fogDepth *  fogDepth * LOG2);
  fogFactor = clamp(fogFactor, 0.0, 1.0);
  fogFactor = (fogFactor + fogFactor * noiseVal) * 0.5; 
  
  gl_FragColor = mix(m_FogColor, texVal, fogFactor);
}
