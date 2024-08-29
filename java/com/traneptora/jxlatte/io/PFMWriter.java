package com.traneptora.jxlatte.io;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import com.traneptora.jxlatte.JXLImage;
import com.traneptora.jxlatte.color.ColorFlags;

public class PFMWriter {
    private final JXLImage image;
    private boolean gray;

    public PFMWriter(JXLImage image) {
        this.image = image;
        this.gray = image.getColorEncoding() == ColorFlags.CE_GRAY;
    }

    public void write(OutputStream out) throws IOException {
        DataOutputStream dout = new DataOutputStream(out);
        // PFM spec requires \n here, not \r\n, so no %n
        int width = image.getWidth();
        int height = image.getHeight();
        String header = String.format("%s\n%d %d\n1.0\n", gray ? "Pf" : "PF",
            image.getWidth(), image.getHeight());
        dout.writeBytes(header);
        float[][] buffer = image.getBuffer(false);
        final int cCount = gray ? 1 : 3;
        // pfm is in backwards scanline order, bottom to top
        for (int y = height - 1; y >= 0; y--) {
             for (int x = 0; x < width; x++) {
                final int p = y * width + x;
                for (int c = 0; c < cCount; c++)
                    dout.writeFloat(buffer[c][p]);
            }
        }
    }
}
