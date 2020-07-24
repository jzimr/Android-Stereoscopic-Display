package jz791.csci366au17.com.stereoscopicdisplay;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioGroup;

import java.util.Random;

public class MainActivity extends AppCompatActivity {
    private ImageView mainImg;              // This is the mainly used image
    private ImageView secondaryImg;         // Another image in the case of the "Stereogram" function
    private LinearLayout linearLayout;      // The LinearLayout we attach the images to

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        linearLayout = findViewById(R.id.linearLayout);

        RadioGroup radioGroup = findViewById(R.id.radioGroup);
        radioGroup.setOnCheckedChangeListener(radioListener);

        mainImg = createImage(R.drawable.ic_launcher_background);    // this is just a placeholder
        mainImg.setImageBitmap(newBitmap(1920, 1080)); // Create our random color image
    }

    private RadioGroup.OnCheckedChangeListener radioListener = new RadioGroup.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(RadioGroup group, int checkedId) {
            // Destroy the secondary image when we switch tabs from "left+right"
            if (checkedId != R.id.radio_left_right && secondaryImg != null){
                linearLayout.removeView(secondaryImg);
                secondaryImg = null;
            }

            switch(checkedId){
                case R.id.radio_random:
                    mainImg.setImageBitmap(newBitmap(1920, 1080));          // Create our random color image
                    break;
                case R.id.radio_stereogram:
                    mainImg.setImageBitmap(generateStereogram(640, 480));     // Create stereogram
                    break;
                case R.id.radio_left_right:                                               // Display left+right image
                    mainImg.setImageResource(R.drawable.left);
                    secondaryImg = createImage(R.drawable.right);
                    break;
                case R.id.radio_anaglyph:
                    mainImg.setImageBitmap(anaglyph(R.drawable.left, R.drawable.right));  // Create anaglyph from left+right image
                    break;
            }
        }
    };

    /*
    Create a stereogram
     */
    private Bitmap generateStereogram(int sizeX, int sizeY){
        Bitmap stereo1 = newBitmap(sizeX/2, sizeY);   // Create half bitmap
        Bitmap stereo2 = Bitmap.createBitmap(stereo1);       // Create duplicate

        // Merge both parts together into one bitmap
        Bitmap stereogram = Bitmap.createBitmap(sizeX, sizeY, Bitmap.Config.ARGB_8888);
        Canvas comboImage = new Canvas(stereogram);
        comboImage.drawBitmap(stereo1, 0f, 0f, null);
        comboImage.drawBitmap(stereo2, stereo1.getWidth(), 0f, null);

        int patchSize = 128;
        int randColor = getRandColor();
        // Create a random value between (-)15 and (-)45
        Random rand = new Random();
        int value = 15 + (rand.nextInt(31));        // Create value between 15 and 45
        int disparity = rand.nextInt(2) == 0 ? value * -1 : value;      // If 0 = -15 to -45. If 1 = 15 to 45

        // Calculate patch positions
        float unitX = sizeX / 6.0f;
        int trackX1 = (int)(unitX);
        int trackX2 = (int) ((unitX * 4) - (patchSize - unitX));
        int trackY = (int)(sizeY / 2.0f - patchSize / 2.0f);

        for(int x = 0; x < patchSize; x++){
            for(int y = 0; y < patchSize; y++){
                // if disparity > 0 = Shift left patch to left, right patch to right
                // if disparity < 0 = Shift left patch to right, right patch to left
                stereogram.setPixel(x + trackX1 - (disparity / 2), y+trackY, randColor);        // left patch
                stereogram.setPixel(x + trackX2 + (disparity / 2), y+trackY, randColor);        // right patch
            }
        }

        // Resize image by 1.4x (To increase pixel size)
        return Bitmap.createScaledBitmap(stereogram, (int)(sizeX * 1.4), (int)(sizeY * 1.4), false);
    }

    /*
    Turn two images into an anaglyph image by setting luminance of left image -> green channel and
    right image -> red + blue channel
     */
    private Bitmap anaglyph(int imageIdX, int imageIdY){
        Bitmap bitmapX = BitmapFactory.decodeResource(getResources(), imageIdX);    // Decode left + right image into a bitmap
        Bitmap bitmapY = BitmapFactory.decodeResource(getResources(), imageIdY);

        int width = bitmapX.getWidth();    // I assume image X and Y have equal dimensions
        int height = bitmapX.getHeight();

        Bitmap anaglyphBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);    // Our new bitmap
        for(int x = 0; x < width; x++){
            for(int y = 0; y < height; y++){
                int pixelValueL = bitmapX.getPixel(x, y);
                int pixelValueR = bitmapY.getPixel(x, y);

                int leftLum = (int)(Color.luminance(pixelValueL)*255);      // Luminosity from left bitmap pixel
                int rightLum = (int)(Color.luminance(pixelValueR)*255);     // Luminosity from right bitmap pixel

                // Set the final pixel with our color channel values
                // (I set the alpha channel here to make it look more clear)
                anaglyphBitmap.setPixel(x, y, android.graphics.Color.argb(175, rightLum, leftLum, rightLum));
            }
        }

        return anaglyphBitmap;  // Return our final product
    }

    /*
    Create an image with specific constraints
     */
    private ImageView createImage(int imageId){
        ImageView image = new ImageView(this);
        image.setImageResource(imageId);    // set image

        // set image position + constraints
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.weight = 1;

        image.setLayoutParams(lp);

        linearLayout.addView(image);        // add view to layout
        return image;                       // return ImageView to caller
    }

    /*
    Generates a bitmap with custom dimension and random colors
     */
    private Bitmap newBitmap(int width, int height){
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        for(int x = 0; x < width; x++){             // Set each pixel in the bitmap
            for(int y = 0; y < height; y++){
                bitmap.setPixel(x, y, getRandColor());
            }
        }
        return bitmap;
    }

    /*
    Generates a random color and returns it
    */
    private int getRandColor(){
        //int a = (int)(Math.random()*256);
        int r = (int)(Math.random()*256);       // Red
        int g = (int)(Math.random()*256);       // Green
        int b = (int)(Math.random()*256);       // Blue

        return android.graphics.Color.argb(255, r, g, b);   // Return color integer value
    }
}
