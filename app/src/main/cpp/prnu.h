#ifndef PRNU_H
#define PRNU_H

#include <jni.h>
#include <opencv2/opencv.hpp>
#include <vector>
#include <string>
#include <utility>

namespace prnu {
    cv::Mat extractNoiseResidual(const cv::Mat& image);
    cv::Mat generateFingerprint(const std::vector<cv::Mat>& images);
    double computeNCC(const cv::Mat& noiseResidual, const cv::Mat& fingerprint);
    std::pair<double, std::string> fallbackAnalysis(const cv::Mat& image);
    bool hasRepetitiveTexture(const cv::Mat& image);
    bool hasOverSmoothing(const cv::Mat& image);
    bool hasFrequencyAnomalies(const cv::Mat& image);
}

extern "C" {

JNIEXPORT jdouble JNICALL
Java_com_example_contentanalyzer_domain_utils_PRNUDetector_nativeExtractNoise(
        JNIEnv* env, jclass clazz, jlong matAddr);

JNIEXPORT jlong JNICALL
Java_com_example_contentanalyzer_domain_utils_PRNUDetector_nativeGenerateFingerprint(
        JNIEnv* env, jclass clazz, jlongArray matAddrs);

JNIEXPORT jdouble JNICALL
Java_com_example_contentanalyzer_domain_utils_PRNUDetector_nativeComputeNCC(
        JNIEnv* env, jclass clazz, jlong noiseAddr, jlong fingerprintAddr);

JNIEXPORT jdoubleArray JNICALL
Java_com_example_contentanalyzer_domain_utils_PRNUDetector_nativeFallbackAnalysis(
        JNIEnv* env, jclass clazz, jlong matAddr);

}

#endif // PRNU_H