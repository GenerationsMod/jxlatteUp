package com.thebombzen.jxlatte.frame.modular;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import com.thebombzen.jxlatte.InvalidBitstreamException;
import com.thebombzen.jxlatte.frame.Frame;
import com.thebombzen.jxlatte.io.Bitreader;

public class ModularStream {

    private static final int[][] kDeltaPalette = {
        {0, 0, 0}, {4, 4, 4}, {11, 0, 0}, {0, 0, -13}, {0, -12, 0}, {-10, -10, -10},
        {-18, -18, -18}, {-27, -27, -27}, {-18, -18, 0}, {0, 0, -32}, {-32, 0, 0}, {-37, -37, -37},
        {0, -32, -32}, {24, 24, 45}, {50, 50, 50}, {-45, -24, -24}, {-24, -45, -45}, {0, -24, -24},
        {-34, -34, 0}, {-24, 0, -24}, {-45, -45, -24}, {64, 64, 64}, {-32, 0, -32}, {0, -32, 0},
        {-32, 0, 32}, {-24, -45, -24}, {45, 24, 45}, {24, -24, -45}, {-45, -24, 24}, {80, 80, 80},
        {64, 0, 0}, {0, 0, -64}, {0, -64, -64}, {-24, -24, 45}, {96, 96, 96}, {64, 64, 0},
        {45, -24, -24}, {34, -34, 0}, {112, 112, 112}, {24, -45, -45}, {45, 45, -24}, {0, -32, 32},
        {24, -24, 45}, {0, 96, 96}, {45, -24, 24}, {24, -45, -24}, {-24, -45, 24}, {0, -64, 0},
        {96, 0, 0}, {128, 128, 128}, {64, 0, 64}, {144, 144, 144}, {96, 96, 0}, {-36, -36, 36},
        {45, -24, -45}, {45, -45, -24}, {0, 0, -96}, {0, 128, 128}, {0, 96, 0}, {45, 24, -45},
        {-128, 0, 0}, {24, -45, 24}, {-45, 24, -45}, {64, 0, -64}, {64, -64, -64}, {96, 0, 96},
        {45, -45, 24}, {24, 45, -45}, {64, 64, -64}, {128, 128, 0}, {0, 0, -128}, {-24, 45, -45} };

    private int channelCount;
    private int nbMetaChannels = 0;
    public final int streamIndex;
    public final int distMultiplier;

    public final MATree tree;
    public final WPHeader wpParams;
    public final TransformInfo[] transforms;
    public final Frame frame;

    private List<ModularChannel> channels = new LinkedList<>();

    public ModularStream(Bitreader reader, MATree globalTree, Frame frame, int streamIndex, int cCount, int ecStart) throws IOException {
        this.channelCount = cCount;
        this.frame = frame;
        this.streamIndex = streamIndex;
        if (channelCount == 0) {
            tree = null;
            wpParams = null;
            transforms = null;
            distMultiplier = 1;
            return;
        }
        boolean useGlobalTree = reader.readBool();
        wpParams = new WPHeader(reader);
        int nbTransforms = reader.readU32(0, 0, 1, 0, 2, 4, 18, 8);
        transforms = new TransformInfo[nbTransforms];
        for (int i = 0; i < nbTransforms; i++)
            transforms[i] = new TransformInfo(reader);
        int w = frame.getFrameHeader().width;
        int h = frame.getFrameHeader().height;
        for (int i = 0; i < channelCount; i++) {
            int dimShift = i < ecStart ? 0 : frame.globalMetadata.getExtraChannelInfo(i - ecStart).dimShift;
            channels.add(new ModularChannel(this, w, h, dimShift));
        }
        for (int i = 0; i < nbTransforms; i++) {
            if (transforms[i].tr == TransformInfo.PALETTE) {
                if (transforms[i].beginC < nbMetaChannels)
                    nbMetaChannels += 2 - transforms[i].numC;
                else
                    nbMetaChannels++;
                int start = transforms[i].beginC + 1;
                for (int j = start; j < transforms[i].beginC + transforms[i].numC; j++)
                    channels.remove(start);
                channels.add(0, new ModularChannel(this, transforms[i].nbColors, transforms[i].numC, -1));
            } else if (transforms[i].tr == TransformInfo.SQUEEZE) {
                int begin = transforms[i].beginC;
                int end = begin + transforms[i].numC - 1;
                for (int j = 0; j < transforms[i].sp.length; j++) {
                    int r = transforms[i].sp[j].inPlace ? end + 1 : channels.size();
                    if (begin < nbMetaChannels) {
                        if (!transforms[i].sp[j].inPlace)
                            throw new InvalidBitstreamException("squeeze meta must be in place");
                        if (end >= nbMetaChannels)
                            throw new InvalidBitstreamException("squeeze meta must end in meta");
                        nbMetaChannels += transforms[i].sp[j].numC;
                    }
                    for (int k = begin; k <= end; k++) {
                        ModularChannel residu;
                        ModularChannel chan = channels.get(k);
                        if (transforms[i].sp[j].horizontal) {
                            w = chan.width;
                            chan.width = (w + 1) / 2;
                            chan.hshift++;
                            residu = new ModularChannel(chan);
                            residu.width = w / 2;
                        } else {
                            h = chan.height;
                            chan.height = (h + 1) / 2;
                            chan.vshift++;
                            residu = new ModularChannel(chan);
                            residu.height = h / 2;
                        }
                        channels.add(r + k - begin, residu);
                    }
                }
            }
        }

        if (!useGlobalTree)
            tree = new MATree(reader);
        else
            tree = globalTree;
        int d = 0;               
        for (int i = nbMetaChannels; i < channels.size(); i++) {
            w = channels.get(i).width;
            if (w > d)
                d = w;
        }
        distMultiplier = d;
    }

    public void decodeChannels(Bitreader reader, boolean partial) throws IOException {
        for (int i = 0; i < channels.size(); i++) {
            ModularChannel chan = channels.get(i);
            if (partial && i >= nbMetaChannels && (chan.width > frame.getFrameHeader().groupDim
                    || chan.height > frame.getFrameHeader().groupDim))
                break;
            chan.decode(reader, tree, i, distMultiplier);
        }
    }

    public void applyTransforms() {
        for (int i = transforms.length - 1; i >= 0; i--) {
            if (transforms[i].tr == TransformInfo.SQUEEZE) {
                throw new UnsupportedOperationException("Squeeze not yet implemented");
            }
            if (transforms[i].tr == TransformInfo.RCT) {
                int permutation = transforms[i].rctType / 7;
                int type = transforms[i].rctType % 7;
                ModularChannel a = channels.get(transforms[i].beginC);
                ModularChannel b = channels.get(transforms[i].beginC + 1);
                ModularChannel c = channels.get(transforms[i].beginC + 2);
                ModularChannel[] v = new ModularChannel[]{a, b, c};
                int w = channels.get(transforms[i].beginC).width;
                int h = channels.get(transforms[i].beginC).height;
                for (int x = 0; x < w; x++) {
                    for (int y = 0; y < h; y++) {
                        int d, e, f;
                        if (type == 6) {
                            int tmp = a.buffer[x][y] - (c.buffer[x][y] >> 1);
                            e = c.buffer[x][y] + tmp;
                            f = tmp - (b.buffer[x][y] >> 1);
                            d = f + b.buffer[x][y];
                        } else {
                            if ((type & 1) != 0)
                                c.buffer[x][y] += a.buffer[x][y];
                            if ((type >> 1) == 1)
                                b.buffer[x][y] += a.buffer[x][y];
                            if ((type >> 1) == 2) 
                                b.buffer[x][y] += (a.buffer[x][y] + c.buffer[x][y]) >> 1;
                            d = a.buffer[x][y];
                            e = b.buffer[x][y];
                            f = c.buffer[x][y];
                        }
                        v[permutation % 3].buffer[x][y] = d;
                        v[(permutation + 1 + (permutation / 3)) % 3].buffer[x][y] = e;
                        v[(permutation + 2 - (permutation / 3)) % 3].buffer[x][y] = f;
                    }
                }
            }
            if (transforms[i].tr == TransformInfo.PALETTE) {
                int first = transforms[i].beginC + 1;
                int endC = transforms[i].beginC + transforms[i].numC - 1;
                int last = endC + 1;
                int bitDepth = frame.globalMetadata.getBitDepthHeader().bitsPerSample;
                ModularChannel firstChannel = channels.get(first);
                for (int j = first + 1; j <= last; j++) {
                    channels.add(j, new ModularChannel(firstChannel));
                }
                for (int c = 0; c < transforms[i].numC; c++) {
                    ModularChannel chan = channels.get(first + c);
                    for (int y = 0; y < firstChannel.height; y++) {
                        for (int x = 0; x < firstChannel.width; x++) {
                            int index = chan.buffer[x][y];
                            boolean isDelta = index < transforms[i].nbDeltas;
                            int value;
                            if (index >= 0 && index < transforms[i].nbColors) {
                                value = channels.get(0).buffer[index][c];
                            } else if (index >= transforms[i].nbColors) {
                                index -= transforms[i].nbColors;
                                if (index < 64) {
                                    value = ((index >> (2 * c)) % 4) * ((1 << bitDepth) - 1) / 4
                                        + (1 << Math.max(0, bitDepth - 3));
                                } else {
                                    index -= 64;
                                    for (int k = 0; k < c; k++)
                                        index /= 5;
                                    value = (index % 5) * ((1 << bitDepth) - 1) / 4;
                                }
                            } else if (c < 3) {
                                index = (-index - 1) % 143;
                                value = kDeltaPalette[(index + 1) >> 1][c];
                                if ((index & 1) == 0)
                                    value = -value;
                                if (bitDepth > 8)
                                    value <<= Math.min(bitDepth, 24) - 8;
                            } else {
                                value = 0;
                            }
                            channels.get(first + c).buffer[x][y] = value;
                            if (isDelta) {
                                chan.buffer[x][y] += chan.prediction(x, y, transforms[i].dPred);
                            }
                        }
                    }
                }
                channels.remove(0);
            }
        }
    }

    public int[][][] getDecodedBuffer() {
        int[][][] buffer = new int[channels.size()][][];
        for (int i = 0; i < buffer.length; i++) {
            buffer[i] = channels.get(i).buffer;
        }
        return buffer;
    }
}