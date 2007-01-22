# Code that creates images for GetMap

import struct, types, zlib

def getSupportedImageFormats():
    """ Returns the list of supported image formats as MIME types """
    return ["image/png", "image/gif"]

##### The routines below are copied or adapted from the minipng library, with
##### thanks to Dan Sandler: see http://dsandler.org/soft/python/minipng.py
##### for original source

##### There is a lot of string concatenation in this code: this could probably
##### be made more efficient using cStringIO

def _flatten(l):
    """Recursively flatten a list.  Ruby 1, Python 0."""
    r = []
    for x in l:
        if type(x) is types.ListType:
            r += _flatten(x)
        else:
            r.append(x)
    return r

# TODO: Internet Explorer does not seem to be able to display these images!  Something
# to do with the alpha channel?  Or the use of an indexed PNG?
def writeIndexedPNG(req, image, opacity):
    """build_indexed_png(image, opacity) -- Return a string containing a
       PNG representing the given image data
 
    req: the request object to which the picture is to be written
    image: a list of rows of pixels; each row is a sequence of pixels;
        each pixel is a single byte corresponding to an index in the palette
    opacity: opacity of the colours in range [0,100]
    """

    palette = get_palette()
    alpha = get_alpha_channel(opacity)
    PNG_CTYPE_INDEXED = 3
    width = len(image[0])
    height = len(image)

    # Write the header bytes
    req.write(struct.pack("8B", 137, 80, 78, 71, 13, 10, 26, 10))
    
    ihdr_data = struct.pack(">LLBBBBB", width, height, 8, PNG_CTYPE_INDEXED, 0, 0, 0)
    
    pal_data = apply(struct.pack, ["%dB" % len(palette * 3)] + _flatten(palette))

    raw_data = apply(struct.pack, ["%dB" % ((width + 1) * height)]
        + _flatten([[0] + image[i] for i in xrange(height)]))
    
    req.write(build_png_chunk("IHDR", ihdr_data))
    req.write(build_png_chunk("PLTE", pal_data))
    req.write(build_png_chunk("tRNS", apply(struct.pack, ["%dB" % len(alpha)] + alpha)))
    req.write(build_png_chunk("IDAT", zlib.compress(raw_data)))
    req.write(build_png_chunk("IEND", ""))

    return

def build_png_chunk(chunk_type, data):
    """build_png_chunk(chunktype, data) -- Construct a PNG chunk.  (Internal.) """
    to_check = chunk_type + data
    return struct.pack('>L', len(data)) \
        + to_check \
        + struct.pack('>L', zlib.crc32(to_check))

def get_palette():
        """ returns a colour palette as a list of 256 values.  The first colour doesn't
            matter because it will be fully transparent (representing missing values).
            The second colour will be black. """

        # This palette was adapted from the Scientific Graphics Toolkit.  It is a rainbow
        # palette, showing large values as red and small values as blue
        return [[0, 0, 0], [0, 0, 0], [0, 0, 143], [0, 0, 146], [0, 0, 150], [0, 0, 154],
                [0, 0, 158], [0, 0, 162], [0, 0, 166], [0, 0, 170], [0, 0, 174], [0, 0, 178],
                [0, 0, 182], [0, 0, 186], [0, 0, 190], [0, 0, 193], [0, 0, 197], [0, 0, 201],
                [0, 0, 205], [0, 0, 209], [0, 0, 213], [0, 0, 217], [0, 0, 221], [0, 0, 225],
                [0, 0, 229], [0, 0, 233], [0, 0, 237], [0, 0, 241], [0, 0, 244], [0, 0, 248],
                [0, 0, 252], [0, 1, 255], [0, 3, 255], [0, 6, 255], [0, 9, 255], [0, 12, 255],
                [0, 16, 255], [0, 20, 255], [0, 24, 255], [0, 28, 255], [0, 31, 255], [0, 35, 255],
                [0, 39, 255], [0, 43, 255], [0, 47, 255], [0, 51, 255], [0, 55, 255], [0, 59, 255],
                [0, 63, 255], [0, 67, 255], [0, 71, 255], [0, 75, 255], [0, 79, 255], [0, 82, 255],
                [0, 86, 255], [0, 90, 255], [0, 94, 255], [0, 98, 255], [0, 102, 255], [0, 106, 255],
                [0, 110, 255], [0, 114, 255], [0, 118, 255], [0, 122, 255], [0, 126, 255], [0, 130, 255],
                [0, 133, 255], [0, 137, 255], [0, 141, 255], [0, 145, 255], [0, 149, 255], [0, 153, 255],
                [0, 157, 255], [0, 161, 255], [0, 165, 255], [0, 169, 255], [0, 173, 255], [0, 177, 255],
                [0, 180, 255], [0, 184, 255], [0, 188, 255], [0, 192, 255], [0, 196, 255], [0, 200, 255],
                [0, 204, 255], [0, 208, 255], [0, 212, 255], [0, 216, 255], [0, 220, 255], [0, 224, 255],
                [0, 228, 255], [0, 231, 255], [0, 235, 255], [0, 239, 255], [0, 243, 255], [0, 247, 255],
                [0, 251, 254], [1, 252, 252], [3, 253, 250], [5, 254, 248], [7, 255, 246], [11, 255, 242],
                [15, 255, 238], [19, 255, 234], [22, 255, 231], [26, 255, 227], [30, 255, 223],
                [34, 255, 219], [38, 255, 215], [42, 255, 211], [46, 255, 207], [50, 255, 203],
                [54, 255, 199], [58, 255, 195], [62, 255, 191], [66, 255, 187], [69, 255, 184],
                [73, 255, 180], [77, 255, 176], [81, 255, 172], [85, 255, 168], [89, 255, 164],
                [93, 255, 160], [97, 255, 156], [101, 255, 152], [105, 255, 148], [109, 255, 144],
                [113, 255, 140], [117, 255, 136], [120, 255, 133], [124, 255, 129], [128, 255, 125],
                [132, 255, 121], [136, 255, 117], [140, 255, 113], [144, 255, 109], [148, 255, 105],
                [152, 255, 101], [156, 255, 97], [160, 255, 93], [164, 255, 89], [168, 255, 85],
                [171, 255, 82], [175, 255, 78], [179, 255, 74], [183, 255, 70], [187, 255, 66],
                [191, 255, 62], [195, 255, 58], [199, 255, 54], [203, 255, 50], [207, 255, 46],
                [211, 255, 42], [215, 255, 38], [218, 255, 35], [222, 255, 31], [226, 255, 27],
                [230, 255, 23], [234, 255, 19], [238, 255, 15], [242, 255, 11], [246, 255, 7],
                [248, 253, 5], [250, 251, 3], [252, 249, 2], [254, 247, 0], [255, 243, 0],
                [255, 240, 0], [255, 236, 0], [255, 232, 0], [255, 228, 0], [255, 224, 0],
                [255, 220, 0], [255, 216, 0], [255, 212, 0], [255, 208, 0], [255, 204, 0],
                [255, 200, 0], [255, 196, 0], [255, 192, 0], [255, 189, 0], [255, 185, 0],
                [255, 181, 0], [255, 177, 0], [255, 173, 0], [255, 169, 0], [255, 165, 0],
                [255, 161, 0], [255, 157, 0], [255, 153, 0], [255, 149, 0], [255, 145, 0],
                [255, 142, 0], [255, 138, 0], [255, 134, 0], [255, 130, 0], [255, 126, 0],
                [255, 122, 0], [255, 118, 0], [255, 114, 0], [255, 110, 0], [255, 106, 0],
                [255, 102, 0], [255, 98, 0], [255, 94, 0], [255, 91, 0], [255, 87, 0],
                [255, 83, 0], [255, 79, 0], [255, 75, 0], [255, 71, 0], [255, 67, 0],
                [255, 63, 0], [255, 59, 0], [255, 55, 0], [255, 51, 0], [255, 47, 0],
                [255, 43, 0], [255, 40, 0], [255, 36, 0], [255, 32, 0], [255, 28, 0],
                [255, 24, 0], [255, 20, 0], [255, 16, 0], [255, 12, 0], [255, 8, 0], [253, 6, 0],
                [251, 4, 0], [249, 2, 0], [247, 0, 0], [243, 0, 0], [239, 0, 0], [235, 0, 0],
                [230, 0, 0], [226, 0, 0], [222, 0, 0], [217, 0, 0], [213, 0, 0], [209, 0, 0],
                [205, 0, 0], [200, 0, 0], [196, 0, 0], [191, 0, 0], [187, 0, 0], [183, 0, 0],
                [178, 0, 0], [174, 0, 0], [170, 0, 0], [165, 0, 0], [161, 0, 0], [157, 0, 0],
                [153, 0, 0], [148, 0, 0], [144, 0, 0], [140, 0, 0]]


def get_alpha_channel(opacity):
        """ returns a set of transparency values, one for each item in the colour palette.
            Pixel at index 0 is fully transparent. """
        alpha = int(2.55 * float(opacity))
        return [0] + [alpha for i in xrange(255)]
