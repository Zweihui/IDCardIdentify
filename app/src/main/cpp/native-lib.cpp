#include <jni.h>
#include <string>
#include "utils.h"
#include <opencv2/opencv.hpp>

#define DEFAULT_CARD_WIDTH 640
#define DEFAULT_CARD_HEIGHT 400
#define FIX_IDCARD_SIZE Size(DEFAULT_CARD_WIDTH,DEFAULT_CARD_HEIGHT)

using namespace std;
using namespace cv;

extern "C" JNIEXPORT jstring JNICALL
Java_com_zwh_idcard_MainActivity_stringFromJNI(
        JNIEnv *env,
        jobject /* this */) {
    std::string hello = "Hello from C++";
    return env->NewStringUTF(hello.c_str());
}
extern "C"
JNIEXPORT jobject JNICALL
Java_com_zwh_idcard_MainActivity_findIdNumber(JNIEnv *env, jobject instance, jobject bitmap,
                                              jobject argb8888) {

    //1、将bitmap转化成mat
    Mat src_img;
    Mat dst_img;
    bitmap2Mat(env,bitmap,&src_img);
    //2、归一化
    Mat dst;
    resize(src_img,dst,FIX_IDCARD_SIZE);
    //3、灰度化处理
    cvtColor(src_img,dst,COLOR_RGB2GRAY);
    //4、二值化处理
    threshold(dst,dst,100,255,THRESH_BINARY);
    //5、图像膨胀
    Mat erodeElement = getStructuringElement(MORPH_RECT,Size(40,10));
    erode(dst,dst,erodeElement);
    //6、轮廓检测
    vector<vector<Point>> contours;
    vector<Rect> rects;
    findContours(dst,contours,RETR_TREE,CHAIN_APPROX_SIMPLE,Point(0,0));
    for(int i=0;i<contours.size();++i){
        Rect rect = boundingRect(contours.at(i));
        rectangle(dst,rect,Scalar(0,0,255));
        //7、根据长宽比筛选出矩形
        if(rect.width>rect.height*8 && rect.width<rect.height*16){
            rects.push_back(rect);
        }
    }
    if(rects.size()<1){
        return NULL;
    }
    //8、选择坐标位置最低的就是身份证号
    int lowPoint = 0;
    Rect idNum;
    for(int i=0;i<rects.size();++i){
        Rect rect = rects.at(i);
        Point point = rect.tl();
        if(point.y>lowPoint){
            lowPoint = point.y;
            idNum = rect;
        }
    }
    //9、图形分割
    dst_img = src_img(idNum);
    //10.资源释放

    return createBitmap(env,dst_img,argb8888);

}extern "C"
JNIEXPORT jobject JNICALL
Java_com_zwh_idcard_MainActivity_findIdName(JNIEnv *env, jobject instance, jobject bitmap,
                                            jobject argb8888) {

    //1、将bitmap转化成mat
    Mat src_img;
    Mat dst_img;
    bitmap2Mat(env,bitmap,&src_img);
    //2、归一化
    Mat dst;
    resize(src_img,dst,FIX_IDCARD_SIZE);
    //3、灰度化处理
    cvtColor(src_img,dst,COLOR_RGB2GRAY);
    //4、二值化处理
    threshold(dst,dst,100,255,THRESH_BINARY);
    //5、图像膨胀
    Mat erodeElement = getStructuringElement(MORPH_RECT,Size(30,10));
    erode(dst,dst,erodeElement);
    //6、轮廓检测
    vector<vector<Point>> contours;
    vector<Rect> rects;
    findContours(dst,contours,RETR_TREE,CHAIN_APPROX_SIMPLE,Point(0,0));
    for(int i=0;i<contours.size();++i){
        Rect rect = boundingRect(contours.at(i));
        rectangle(dst,rect,Scalar(0,0,255));
        //7、根据长宽比筛选出矩形
        if(rect.width>rect.height*3 && rect.width<rect.height*4){
            rects.push_back(rect);
        }
    }
    if(rects.size()<1){
        return NULL;
    }
    //8、选择坐标位置最低的就是身份证号
    int heightPoint;
    Rect idName;
    for(int i=0;i<rects.size();++i){
        Rect rect = rects.at(i);
        Point point = rect.tl();
        if(i == 0){
            heightPoint = point.y;
        }
        if(point.y<heightPoint){
            heightPoint = point.y;
            idName = rect;
        }
    }
    //9、图形分割
    dst_img = src_img(idName);
    //10.资源释放

    return createBitmap(env,dst_img,argb8888);

}