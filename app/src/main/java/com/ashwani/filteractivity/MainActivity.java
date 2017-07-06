package com.ashwani.filteractivity;

import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PointF;
import android.media.ExifInterface;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import jp.co.cyberagent.android.gpuimage.*;

public class MainActivity extends AppCompatActivity implements RecyclerViewItemClickListener{


    RecyclerView recyclerView;
    MarshmallowPermission marshmallowPermission = new MarshmallowPermission(this);
    private final int SELECT_PHOTO = 1;
    Uri imageUri;
    List<Filters> filters = new ArrayList<>();

    private GPUImageFilter mFilter;
    private GPUImageView mGPUImageView;

    @Override
    public void onClick(int position) {
        switchFilterTo(filters.get(position).getGpuImageFilter());
        mGPUImageView.requestRender();
    }

    private enum FilterType {
        CONTRAST, GRAYSCALE, SHARPEN, SEPIA, SOBEL_EDGE_DETECTION, THREE_X_THREE_CONVOLUTION, FILTER_GROUP, EMBOSS, POSTERIZE, GAMMA, BRIGHTNESS, INVERT, HUE, PIXELATION,
        SATURATION, EXPOSURE, HIGHLIGHT_SHADOW, MONOCHROME, OPACITY, RGB, WHITE_BALANCE, VIGNETTE, TONE_CURVE, LOOKUP_AMATORKA,
        GAUSSIAN_BLUR, CROSSHATCH, BOX_BLUR, CGA_COLORSPACE, DILATION, KUWAHARA, RGB_DILATION, SKETCH, TOON, SMOOTH_TOON, BULGE_DISTORTION, GLASS_SPHERE, HAZE, LAPLACIAN, NON_MAXIMUM_SUPPRESSION,
        SPHERE_REFRACTION, SWIRL, WEAK_PIXEL_INCLUSION, FALSE_COLOR, COLOR_BALANCE, LEVELS_FILTER_MIN, BILATERAL_BLUR, HALFTONE, TRANSFORM2D
    }
    private void switchFilterTo(final GPUImageFilter filter) {
        if (mFilter == null
                || (filter != null && !mFilter.getClass().equals(filter.getClass()))) {
            mFilter = filter;
            mGPUImageView.setFilter(mFilter);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        marshmallowPermission.requestMultiplePermission();

        mGPUImageView = (GPUImageView) findViewById(R.id.imageView);
        recyclerView = (RecyclerView) findViewById(R.id.filterList);

        populateFilters();

        LinearLayoutManager mLinearLayoutManager = new LinearLayoutManager(this);
        mLinearLayoutManager.setOrientation(LinearLayoutManager.HORIZONTAL);
        recyclerView.setLayoutManager(mLinearLayoutManager);

        FilterAdapter filterAdapter = new FilterAdapter(this, filters, this);
        recyclerView.setAdapter(filterAdapter);

        mGPUImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //TODO pick image
                Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
                photoPickerIntent.setType("image/*");
                startActivityForResult(photoPickerIntent, SELECT_PHOTO);
            }
        });

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case SELECT_PHOTO:
                if (resultCode == RESULT_OK) {
                    imageUri = data.getData();
                    new Thread() {
                        @Override
                        public void run() {
                            //running image processing on different thread
                            setImageToImageView(compressImage(imageUri));
                        }
                    }.start();
                }
        }
    }

    public void setImageToImageView(Bitmap bitmap) {
        mGPUImageView.setImage(bitmap);
    }

    public Bitmap compressImage(Uri imageUri) {

        String filePath = getRealPathFromURI(imageUri);
        Bitmap scaledBitmap = null;

        BitmapFactory.Options options = new BitmapFactory.Options();

//      by setting this field as true, the actual bitmap pixels are not loaded in the memory. Just the bounds are loaded. If
//      you try the use the bitmap here, you will get null.
        options.inJustDecodeBounds = true;
        Bitmap bmp = BitmapFactory.decodeFile(filePath, options);

        int actualHeight = options.outHeight;
        int actualWidth = options.outWidth;

//      max Height and width values of the compressed image is taken as 816x612

        float maxHeight = 1920.0f;
        float maxWidth = 1080.0f;
        float imgRatio = actualWidth / actualHeight;
        float maxRatio = maxWidth / maxHeight;

//      width and height values are set maintaining the aspect ratio of the image

        if (actualHeight > maxHeight || actualWidth > maxWidth) {
            if (imgRatio < maxRatio) {
                imgRatio = maxHeight / actualHeight;
                actualWidth = (int) (imgRatio * actualWidth);
                actualHeight = (int) maxHeight;
            } else if (imgRatio > maxRatio) {
                imgRatio = maxWidth / actualWidth;
                actualHeight = (int) (imgRatio * actualHeight);
                actualWidth = (int) maxWidth;
            } else {
                actualHeight = (int) maxHeight;
                actualWidth = (int) maxWidth;

            }
        }

//      setting inSampleSize value allows to load a scaled down version of the original image

        options.inSampleSize = calculateInSampleSize(options, actualWidth, actualHeight);

//      inJustDecodeBounds set to false to load the actual bitmap
        options.inJustDecodeBounds = false;

//      this options allow android to claim the bitmap memory if it runs low on memory
        options.inPurgeable = true;
        options.inInputShareable = true;
        options.inTempStorage = new byte[16 * 1024];

        try {
//          load the bitmap from its path
            bmp = BitmapFactory.decodeFile(filePath, options);
        } catch (OutOfMemoryError exception) {
            exception.printStackTrace();

        }
        try {
            scaledBitmap = Bitmap.createBitmap(actualWidth, actualHeight, Bitmap.Config.ARGB_8888);
        } catch (OutOfMemoryError exception) {
            exception.printStackTrace();
        }

        float ratioX = actualWidth / (float) options.outWidth;
        float ratioY = actualHeight / (float) options.outHeight;
        float middleX = actualWidth / 2.0f;
        float middleY = actualHeight / 2.0f;

        Matrix scaleMatrix = new Matrix();
        scaleMatrix.setScale(ratioX, ratioY, middleX, middleY);

        Canvas canvas = new Canvas(scaledBitmap);
        canvas.setMatrix(scaleMatrix);
        canvas.drawBitmap(bmp, middleX - bmp.getWidth() / 2, middleY - bmp.getHeight() / 2, new Paint(Paint.FILTER_BITMAP_FLAG));

//      check the rotation of the image and display it properly
        ExifInterface exif;
        try {
            exif = new ExifInterface(filePath);

            int orientation = exif.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION, 0);
            Log.d("EXIF", "Exif: " + orientation);
            Matrix matrix = new Matrix();
            if (orientation == 6) {
                matrix.postRotate(90);
                Log.d("EXIF", "Exif: " + orientation);
            } else if (orientation == 3) {
                matrix.postRotate(180);
                Log.d("EXIF", "Exif: " + orientation);
            } else if (orientation == 8) {
                matrix.postRotate(270);
                Log.d("EXIF", "Exif: " + orientation);
            }
            scaledBitmap = Bitmap.createBitmap(scaledBitmap, 0, 0,
                    scaledBitmap.getWidth(), scaledBitmap.getHeight(), matrix,
                    true);
            return scaledBitmap;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private String getRealPathFromURI(Uri contentURI) {
        Uri contentUri = contentURI;
        Cursor cursor = getContentResolver().query(contentUri, null, null, null, null);
        if (cursor == null) {
            return contentUri.getPath();
        } else {
            cursor.moveToFirst();
            int index = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA);
            return cursor.getString(index);
        }
    }

    public int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            final int heightRatio = Math.round((float) height / (float) reqHeight);
            final int widthRatio = Math.round((float) width / (float) reqWidth);
            inSampleSize = heightRatio < widthRatio ? heightRatio : widthRatio;
        }
        final float totalPixels = width * height;
        final float totalReqPixelsCap = reqWidth * reqHeight * 2;
        while (totalPixels / (inSampleSize * inSampleSize) > totalReqPixelsCap) {
            inSampleSize++;
        }

        return inSampleSize;
    }

    public void addFilter(String name, FilterType filterType){
        Filters filter = new Filters();
        filter.setName(name);
        filter.setGpuImageFilter(createFilterForType(filterType));
        this.filters.add(filter);
    }

    public void populateFilters() {

        addFilter("Contrast", FilterType.CONTRAST);
        addFilter("Invert", FilterType.INVERT);
        addFilter("Pixelation", FilterType.PIXELATION);
        addFilter("Hue", FilterType.HUE);
        addFilter("Gamma", FilterType.GAMMA);
        addFilter("Brightness", FilterType.BRIGHTNESS);
        addFilter("Sepia", FilterType.SEPIA);
        addFilter("Grayscale", FilterType.GRAYSCALE);
        addFilter("Sharpness", FilterType.SHARPEN);
        addFilter("Sobel Edge Detection", FilterType.SOBEL_EDGE_DETECTION);
        addFilter("3x3 Convolution", FilterType.THREE_X_THREE_CONVOLUTION);
        addFilter("Emboss", FilterType.EMBOSS);
        addFilter("Posterize", FilterType.POSTERIZE);
        addFilter("Grouped filters", FilterType.FILTER_GROUP);
        addFilter("Saturation", FilterType.SATURATION);
        addFilter("Exposure", FilterType.EXPOSURE);
        addFilter("Highlight Shadow", FilterType.HIGHLIGHT_SHADOW);
        addFilter("Monochrome", FilterType.MONOCHROME);
        addFilter("Opacity", FilterType.OPACITY);
        addFilter("RGB", FilterType.RGB);
        addFilter("White Balance", FilterType.WHITE_BALANCE);
        addFilter("Vignette", FilterType.VIGNETTE);
        addFilter("ToneCurve", FilterType.TONE_CURVE);

        addFilter("Lookup (Amatorka)", FilterType.LOOKUP_AMATORKA);
        addFilter("Gaussian Blur", FilterType.GAUSSIAN_BLUR);
        addFilter("Crosshatch", FilterType.CROSSHATCH);

        addFilter("Box Blur", FilterType.BOX_BLUR);
        addFilter("CGA Color Space", FilterType.CGA_COLORSPACE);
        addFilter("Dilation", FilterType.DILATION);
        addFilter("Kuwahara", FilterType.KUWAHARA);
        addFilter("RGB Dilation", FilterType.RGB_DILATION);
        addFilter("Sketch", FilterType.SKETCH);
        addFilter("Toon", FilterType.TOON);
        addFilter("Smooth Toon", FilterType.SMOOTH_TOON);
        addFilter("Halftone", FilterType.HALFTONE);

        addFilter("Bulge Distortion", FilterType.BULGE_DISTORTION);
        addFilter("Glass Sphere", FilterType.GLASS_SPHERE);
        addFilter("Haze", FilterType.HAZE);
        addFilter("Laplacian", FilterType.LAPLACIAN);
        addFilter("Non Maximum Suppression", FilterType.NON_MAXIMUM_SUPPRESSION);
        addFilter("Sphere Refraction", FilterType.SPHERE_REFRACTION);
        addFilter("Swirl", FilterType.SWIRL);
        addFilter("Weak Pixel Inclusion", FilterType.WEAK_PIXEL_INCLUSION);
        addFilter("False Color", FilterType.FALSE_COLOR);

        addFilter("Color Balance", FilterType.COLOR_BALANCE);

        addFilter("Levels Min (Mid Adjust)", FilterType.LEVELS_FILTER_MIN);

         addFilter("Bilateral Blur", FilterType.BILATERAL_BLUR);

        addFilter("Transform (2-D)", FilterType.TRANSFORM2D);

    }

    private GPUImageFilter createFilterForType(final FilterType type) {
        switch (type) {
            case CONTRAST:
                return new GPUImageContrastFilter(2.0f);
            case GAMMA:
                return new GPUImageGammaFilter(2.0f);
            case INVERT:
                return new GPUImageColorInvertFilter();
            case PIXELATION:
                return new GPUImagePixelationFilter();
            case HUE:
                return new GPUImageHueFilter(90.0f);
            case BRIGHTNESS:
                return new GPUImageBrightnessFilter(1.5f);
            case GRAYSCALE:
                return new GPUImageGrayscaleFilter();
            case SEPIA:
                return new GPUImageSepiaFilter();
            case SHARPEN:
                GPUImageSharpenFilter sharpness = new GPUImageSharpenFilter();
                sharpness.setSharpness(2.0f);
                return sharpness;
            case SOBEL_EDGE_DETECTION:
                return new GPUImageSobelEdgeDetection();
            case THREE_X_THREE_CONVOLUTION:
                GPUImage3x3ConvolutionFilter convolution = new GPUImage3x3ConvolutionFilter();
                convolution.setConvolutionKernel(new float[]{
                        -1.0f, 0.0f, 1.0f,
                        -2.0f, 0.0f, 2.0f,
                        -1.0f, 0.0f, 1.0f
                });
                return convolution;
            case EMBOSS:
                return new GPUImageEmbossFilter();
            case POSTERIZE:
                return new GPUImagePosterizeFilter();
            case FILTER_GROUP:
                List<GPUImageFilter> filters = new ArrayList<>();
                filters.add(new GPUImageContrastFilter());
                filters.add(new GPUImageDirectionalSobelEdgeDetectionFilter());
                filters.add(new GPUImageGrayscaleFilter());
                return new GPUImageFilterGroup(filters);
            case SATURATION:
                return new GPUImageSaturationFilter(1.0f);
            case EXPOSURE:
                return new GPUImageExposureFilter(0.0f);
            case HIGHLIGHT_SHADOW:
                return new GPUImageHighlightShadowFilter(0.0f, 1.0f);
            case MONOCHROME:
                return new GPUImageMonochromeFilter(1.0f, new float[]{0.6f, 0.45f, 0.3f, 1.0f});
            case OPACITY:
                return new GPUImageOpacityFilter(1.0f);
            case RGB:
                return new GPUImageRGBFilter(1.0f, 1.0f, 1.0f);
            case WHITE_BALANCE:
                return new GPUImageWhiteBalanceFilter(5000.0f, 0.0f);
            case VIGNETTE:
                PointF centerPoint = new PointF();
                centerPoint.x = 0.5f;
                centerPoint.y = 0.5f;
                return new GPUImageVignetteFilter(centerPoint, new float[]{0.0f, 0.0f, 0.0f}, 0.3f, 0.75f);
            case TONE_CURVE:
                GPUImageToneCurveFilter toneCurveFilter = new GPUImageToneCurveFilter();
                toneCurveFilter.setFromCurveFileInputStream(
                        getResources().openRawResource(R.raw.tone_cuver_sample));
                return toneCurveFilter;

            case LOOKUP_AMATORKA:
                GPUImageLookupFilter amatorka = new GPUImageLookupFilter();
                amatorka.setBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.lookup_amatorka));
                return amatorka;
            case GAUSSIAN_BLUR:
                return new GPUImageGaussianBlurFilter();
            case CROSSHATCH:
                return new GPUImageCrosshatchFilter();

            case BOX_BLUR:
                return new GPUImageBoxBlurFilter();
            case CGA_COLORSPACE:
                return new GPUImageCGAColorspaceFilter();
            case DILATION:
                return new GPUImageDilationFilter();
            case KUWAHARA:
                return new GPUImageKuwaharaFilter();
            case RGB_DILATION:
                return new GPUImageRGBDilationFilter();
            case SKETCH:
                return new GPUImageSketchFilter();
            case TOON:
                return new GPUImageToonFilter();
            case SMOOTH_TOON:
                return new GPUImageSmoothToonFilter();

            case BULGE_DISTORTION:
                return new GPUImageBulgeDistortionFilter();
            case GLASS_SPHERE:
                return new GPUImageGlassSphereFilter();
            case HAZE:
                return new GPUImageHazeFilter();
            case LAPLACIAN:
                return new GPUImageLaplacianFilter();
            case NON_MAXIMUM_SUPPRESSION:
                return new GPUImageNonMaximumSuppressionFilter();
            case SPHERE_REFRACTION:
                return new GPUImageSphereRefractionFilter();
            case SWIRL:
                return new GPUImageSwirlFilter();
            case WEAK_PIXEL_INCLUSION:
                return new GPUImageWeakPixelInclusionFilter();
            case FALSE_COLOR:
                return new GPUImageFalseColorFilter();
            case COLOR_BALANCE:
                return new GPUImageColorBalanceFilter();
            case LEVELS_FILTER_MIN:
                GPUImageLevelsFilter levelsFilter = new GPUImageLevelsFilter();
                levelsFilter.setMin(0.0f, 3.0f, 1.0f);
                return levelsFilter;
            case HALFTONE:
                return new GPUImageHalftoneFilter();

            case BILATERAL_BLUR:
                return new GPUImageBilateralFilter();

            case TRANSFORM2D:
                return new GPUImageTransformFilter();

            default:
                throw new IllegalStateException("No filter of that type!");
        }
    }

}
