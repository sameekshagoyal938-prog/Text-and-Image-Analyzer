#include "prnu.h"
#include <android/log.h>
#include <cmath>
#include <numeric>
#include <exception>

#define LOG_TAG "PRNU_CPP"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

namespace prnu {

    cv::Mat extractNoiseResidual(const cv::Mat& image) {
        if (image.empty()) return cv::Mat();

        cv::Mat gray;
        if (image.channels() == 3) {
            cv::cvtColor(image, gray, cv::COLOR_BGR2GRAY);
        } else if (image.channels() == 4) {
            cv::cvtColor(image, gray, cv::COLOR_BGRA2GRAY);
        } else {
            gray = image.clone();
        }

        cv::Mat floatImg;
        gray.convertTo(floatImg, CV_32F);

        cv::Mat denoised;
        cv::GaussianBlur(floatImg, denoised, cv::Size(7, 7), 1.5);

        cv::Mat noise;
        cv::subtract(floatImg, denoised, noise);

        cv::Scalar mean = cv::mean(noise);
        cv::subtract(noise, mean, noise);

        cv::Scalar stddev;
        cv::meanStdDev(noise, mean, stddev);
        if (stddev[0] > 1e-6) {
            noise /= stddev[0];
        }

        return noise;
    }

    cv::Mat generateFingerprint(const std::vector<cv::Mat>& images) {
        if (images.empty()) return cv::Mat();

        cv::Mat sumNoise;
        int validCount = 0;

        for (const auto& img : images) {
            cv::Mat noise = extractNoiseResidual(img);
            if (noise.empty()) continue;

            if (sumNoise.empty()) {
                sumNoise = cv::Mat::zeros(noise.size(), noise.type());
            }

            if (noise.size() == sumNoise.size()) {
                cv::add(sumNoise, noise, sumNoise);
                validCount++;
            }
        }

        if (validCount == 0) return cv::Mat();

        cv::Mat fingerprint = sumNoise / validCount;
        return fingerprint;
    }

    double computeNCC(const cv::Mat& noiseResidual, const cv::Mat& fingerprint) {
        if (noiseResidual.empty() || fingerprint.empty()) return 0.0;

        cv::Mat fpResized;
        if (noiseResidual.size() != fingerprint.size()) {
            cv::resize(fingerprint, fpResized, noiseResidual.size());
        } else {
            fpResized = fingerprint;
        }

        cv::Mat result;
        cv::matchTemplate(noiseResidual, fpResized, result, cv::TM_CCOEFF_NORMED);

        double minVal, maxVal;
        cv::minMaxLoc(result, &minVal, &maxVal);
        return maxVal;
    }

    bool hasRepetitiveTexture(const cv::Mat& image) {
        if (image.empty()) return false;
        cv::Mat gray;
        if (image.channels() > 1) cv::cvtColor(image, gray, cv::COLOR_BGR2GRAY);
        else gray = image;

        cv::Mat variance, mean;
        cv::boxFilter(gray, mean, CV_32F, cv::Size(15, 15));
        cv::Mat gray2;
        gray.convertTo(gray2, CV_32F);
        cv::boxFilter(gray2.mul(gray2), variance, CV_32F, cv::Size(15, 15));
        variance = variance - mean.mul(mean);

        double maxVal;
        cv::minMaxLoc(variance, nullptr, &maxVal);
        return maxVal < 100.0; // Simple heuristic
    }

    bool hasOverSmoothing(const cv::Mat& image) {
        if (image.empty()) return false;
        cv::Mat gray;
        if (image.channels() > 1) cv::cvtColor(image, gray, cv::COLOR_BGR2GRAY);
        else gray = image;

        cv::Mat gradX, gradY;
        cv::Sobel(gray, gradX, CV_32F, 1, 0, 3);
        cv::Sobel(gray, gradY, CV_32F, 0, 1, 3);
        cv::Mat gradMag;
        cv::magnitude(gradX, gradY, gradMag);
        return cv::mean(gradMag)[0] < 10.0;
    }

    bool hasFrequencyAnomalies(const cv::Mat& image) {
        return false; // Placeholder
    }

    std::pair<double, std::string> fallbackAnalysis(const cv::Mat& image) {
        double confidence = 70.0;
        std::string reason = "Analysis: ";

        if (hasRepetitiveTexture(image)) {
            confidence -= 20.0;
            reason += "Repetitive patterns; ";
        }
        if (hasOverSmoothing(image)) {
            confidence -= 15.0;
            reason += "Over-smoothing; ";
        }

        return {std::max(10.0, confidence), reason};
    }

} // namespace prnu

extern "C" {

JNIEXPORT jdouble JNICALL
Java_com_example_contentanalyzer_domain_utils_PRNUDetector_nativeExtractNoise(
        JNIEnv* env, jclass clazz, jlong matAddr) {
    try {
        cv::Mat* mat = reinterpret_cast<cv::Mat*>(matAddr);
        if (!mat || mat->empty()) return 0.0;
        cv::Mat noise = prnu::extractNoiseResidual(*mat);
        cv::Scalar mean, stddev;
        cv::meanStdDev(noise, mean, stddev);
        return stddev[0];
    } catch (...) { return 0.0; }
}

JNIEXPORT jlong JNICALL
Java_com_example_contentanalyzer_domain_utils_PRNUDetector_nativeGenerateFingerprint(
        JNIEnv* env, jclass clazz, jlongArray matAddrs) {
    try {
        jsize len = env->GetArrayLength(matAddrs);
        std::vector<cv::Mat> images;
        jlong* addrs = env->GetLongArrayElements(matAddrs, nullptr);
        for (int i = 0; i < len; i++) {
            cv::Mat* mat = reinterpret_cast<cv::Mat*>(addrs[i]);
            if (mat && !mat->empty()) images.push_back(*mat);
        }
        env->ReleaseLongArrayElements(matAddrs, addrs, JNI_ABORT);
        cv::Mat fingerprint = prnu::generateFingerprint(images);
        if (fingerprint.empty()) return 0L;
        return reinterpret_cast<jlong>(new cv::Mat(fingerprint));
    } catch (...) { return 0L; }
}

JNIEXPORT jdouble JNICALL
Java_com_example_contentanalyzer_domain_utils_PRNUDetector_nativeComputeNCC(
        JNIEnv* env, jclass clazz, jlong noiseAddr, jlong fingerprintAddr) {
    try {
        cv::Mat* noise = reinterpret_cast<cv::Mat*>(noiseAddr);
        cv::Mat* fingerprint = reinterpret_cast<cv::Mat*>(fingerprintAddr);
        if (!noise || !fingerprint) return 0.0;
        return prnu::computeNCC(*noise, *fingerprint);
    } catch (...) { return 0.0; }
}

JNIEXPORT jdoubleArray JNICALL
Java_com_example_contentanalyzer_domain_utils_PRNUDetector_nativeFallbackAnalysis(
        JNIEnv* env, jclass clazz, jlong matAddr) {
    try {
        LOGD("nativeFallbackAnalysis called with matAddr: %lld", (long long)matAddr);
        cv::Mat* mat = reinterpret_cast<cv::Mat*>(matAddr);
        jdoubleArray result = env->NewDoubleArray(2);
        if (!mat || mat->empty()) {
            jdouble values[] = {50.0, 0.0};
            env->SetDoubleArrayRegion(result, 0, 2, values);
            return result;
        }
        auto fallback = prnu::fallbackAnalysis(*mat);
        jdouble values[] = {fallback.first, 0.0};
        env->SetDoubleArrayRegion(result, 0, 2, values);
        return result;
    } catch (const std::exception& e) {
        LOGE("Exception in nativeFallbackAnalysis: %s", e.what());
        return nullptr;
    } catch (...) {
        LOGE("Unknown exception in nativeFallbackAnalysis");
        return nullptr;
    }
}

} // extern "C"