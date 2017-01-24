/*
 * Copyright (C) 2012 CyberAgent
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package jp.co.cyberagent.android.gpuimage;

import android.graphics.PointF;
import android.opengl.GLES20;
import android.util.Log;

public class GPUImageMagFilter extends GPUImageFilter {
    public static final String MAG_FRAGMENT_SHADER = " precision highp float;\n" +
            " \n" +
            " uniform sampler2D inputImageTexture;\n" +
            " varying highp vec2 textureCoordinate;\n" +
            "\n" +
            " uniform float thresholdSensitivity;\n" +
            " uniform float smoothing;\n" +
            " uniform float magnification;\n" +
            " uniform vec3 colorToReplace;\n" +
            " \n" +
            " vec3 RGB_to_YCrCb(lowp vec3 rgb) {\n" +
            "     float Y = 0.2989 * rgb.r + 0.5866 * rgb.g + 0.1145 * rgb.b;\n" +
            "     float Cr = 0.7132 * (rgb.r - Y);\n" +
            "     float Cb = 0.5647 * (rgb.b - Y);\n" +
            "     return vec3(Y,Cr,Cb);\n" +
            "  }\n" +
            " \n" +
            " void main()\n" +
            " {\n" +
            "     lowp vec4 textureColor = texture2D(inputImageTexture, textureCoordinate);\n" +
            "     lowp vec2 magCoordinate = vec2(0.5,0.5) + (textureCoordinate-vec2(0.5,0.5)) * magnification;\n" +
            "     lowp vec4 magColor = texture2D(inputImageTexture, magCoordinate);\n" +
            "     \n" +
            "     vec3 maskYCrCb = RGB_to_YCrCb(colorToReplace);\n" +
            "     vec3 magYCrCb = RGB_to_YCrCb(magColor.rgb);\n" +
            "     vec3 YCrCb = RGB_to_YCrCb(textureColor.rgb);\n" +
            "     \n" +
            "     float my_distance = distance(vec2(YCrCb.y, YCrCb.z), vec2(maskYCrCb.y, maskYCrCb.z));\n" +
            "     textureColor *= step(thresholdSensitivity, my_distance);\n" +
            "     \n" +
            "     lowp float mag_distance = distance(vec2(magYCrCb.y, magYCrCb.z), vec2(maskYCrCb.y, maskYCrCb.z));\n" +
            "     lowp float blendValue = 1.0 - smoothstep(thresholdSensitivity, thresholdSensitivity + smoothing, mag_distance);\n" +
            "     gl_FragColor = mix(textureColor, magColor, blendValue*0.7);\n" +
            " }";

    private int mThresholdSensitivityLocation;
    private int mSmoothingLocation;
    private int mMagnificationLocation;
    private int mColorToReplaceLocation;

    private float mSmoothing = 0.1f;
    private float mMagnification = 0.3f;
    private float mThresholdSensitivity = 0.3f;
    private float[] mColorToReplace = new float[]{1.0f, 0.0f, 0.0f};

    public GPUImageMagFilter(final float[] colorToReplace) {
        super(NO_FILTER_VERTEX_SHADER, MAG_FRAGMENT_SHADER);
        mColorToReplace = colorToReplace;
    }

    @Override
    public void onInit() {
        super.onInit();
        mThresholdSensitivityLocation = GLES20.glGetUniformLocation(getProgram(), "thresholdSensitivity");
        mSmoothingLocation = GLES20.glGetUniformLocation(getProgram(), "smoothing");
        mMagnificationLocation = GLES20.glGetUniformLocation(getProgram(), "magnification");
        mColorToReplaceLocation = GLES20.glGetUniformLocation(getProgram(), "colorToReplace");
    }

    @Override
    public void onInitialized() {
        super.onInitialized();
        setSmoothing(mSmoothing);
        setMagnification(mMagnification);
        setThresholdSensitivity(mThresholdSensitivity);
        setColorToReplace(mColorToReplace[0], mColorToReplace[1], mColorToReplace[2]);
    }

    /**
     * The degree of smoothing controls how gradually similar colors are replaced in the image
     * The default value is 0.1
     */
    public void setSmoothing(final float smoothing) {
        mSmoothing = smoothing;
        setFloat(mSmoothingLocation, mSmoothing);
    }

    public void setMagnification(final float magnification) {
        mMagnification = magnification;
        setFloat(mMagnificationLocation, mMagnification);
    }


    /**
     * The threshold sensitivity controls how similar pixels need to be colored to be replaced
     * The default value is 0.3
     */
    public void setThresholdSensitivity(final float thresholdSensitivity) {
        Log.d("threshold", "set: " + thresholdSensitivity);
        mThresholdSensitivity = thresholdSensitivity;
        setFloat(mThresholdSensitivityLocation, mThresholdSensitivity);
    }

    /** The color to be replaced is specified using individual red, green, and blue components (normalized to 1.0).
     * The default is green: (0.0, 1.0, 0.0).
     *
     * @param redComponent Red component of color to be replaced
     * @param greenComponent Green component of color to be replaced
     * @param blueComponent Blue component of color to be replaced
     */
    public void setColorToReplace(float redComponent, float greenComponent, float blueComponent) {
        mColorToReplace = new float[]{redComponent, greenComponent, blueComponent};
        setFloatVec3(mColorToReplaceLocation, mColorToReplace);
    }
}
