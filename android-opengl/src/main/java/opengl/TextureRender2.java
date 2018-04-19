/*
 * TextureRender2.java
 * bundle-opengl
 *
 * Copyright (C) 2018, Gleb Nikitenko. All Rights Reserved.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
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
@SuppressWarnings("WeakerAccess")
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
    @SuppressWarnings("EmptyMethod")
    public void surfaceCreated() {}

    /** {@inheritDoc} */
    @Override public final void close() {
        Program2d.closeProgram(mProgram);
        Texture2d.closeTextures(mTextures);
    }
}