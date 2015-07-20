package p79068.bmpio;

/**
 * Created by jsybrand on 7/20/15.
 */
public class Rgb888ImageArray implements Rgb888Image {

    private int width,height;
    private int data[][];

    public Rgb888ImageArray(int width, int height,int backgroundColor)
    {
        this.width=width;
        this.height=height;
        data = new int[width][height];
        for(int i = 0 ; i < width; i ++)
            for(int j = 0 ; j < height ; j++)
                data[i][j]=backgroundColor;
    }

    public int getWidth(){return width;}


    public int getHeight(){return height;}


    public int getRgb888Pixel(int x, int y){
        return data[x][y];
    }

    public void setRgb888Pixel(int x, int y, byte r, byte g, byte b)
    {
        setRgb888Pixel(x,y,getColor(r,g,b));
    }

    public void setRgb888Pixel(int x, int y, int color)
    {
        data[x][y]= color;
    }

    public static int getColor(byte r, byte g, byte b)
    {
        return (r<<16 & 0xFF0000) & (g<<8 & 0xFF00) & (b & 0xFF);
    }

}
