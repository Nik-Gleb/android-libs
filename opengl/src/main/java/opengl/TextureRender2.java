/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package opengl;
import android.graphics.SurfaceTexture;

import java.io.Closeable;

import proguard.annotation.Keep;
import proguard.annotation.KeepPublicClassMembers;

/**
 * Code for rendering a texture onto a surface using OpenGL ES 2.0.
 */
@Keep
@KeepPublicClassMembers
public class TextureRender2 implements Closeable {

    private final int INDEX = 0;
    private final boolean[] mTargets = {true};
    private final float[] mSTMatrix = Program2d.createIdentityMatrix();
    private final int[] mTemp = new int[3],
        mTextures = Texture2d.createTextures(mTargets, mTemp),
        mProgram = Program2d.createProgram(mTargets[INDEX]);


    public int getTextureId() {
        return mTextures[INDEX];
    }

    public void drawFrame(SurfaceTexture st) {
        st.getTransformMatrix(mSTMatrix);
        Program2d.draw(mProgram, mSTMatrix, INDEX);
    }

    /**
     * Initializes GL state.  Call this after the EGL surface has been created and made current.
     */
    public void surfaceCreated() {}

    /** {@inheritDoc} */
    @Override public final void close() {
        Program2d.closeProgram(mProgram);
        Texture2d.closeTextures(mTextures);
    }
}