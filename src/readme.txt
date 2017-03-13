MyCompression expects to be called on an rgb or raw image.
The arguments expected are a file ending with .raw or .rgb and an integer n value
For example MyCompression /file.rgb 16

MyCompression is using kMeans++ to generate a codebook of size n.
The random functions of kMeans++ are seeded to the same value so multiple
calls of MyCompression with the same input will give you the same looking 
compressed image.

MyCompression outputs two images. The leftmost image is the original uncompressed
image and the right is the image after compression. 
