package com.sxdbsm.cookbook.android.ai

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import com.sxdbsm.cookbook.android.util.AppLogger

/**
 * @File : VoiceRecognizer
 * @Time : 2026/07/29
 * @Author : SXD-AI
 * @Desc : Android SpeechRecognizer 封装，供 AI 快捷输入记餐语音转文字使用
 * <p>
 * 封装 Android 系统 SpeechRecognizer：
 * - startListening() 开始录音识别
 * - stopListening() 停止并等待最终结果
 * - cancelListening() 取消识别
 * - destroy() 释放资源
 * 通过回调将部分识别结果和最终结果通知调用方。
 * 录音仅在本地识别，不上传语音文件（隐私安全）。
 * <p>
 * [AI生成] K2 AI快捷输入记餐语音修复：集成系统 SpeechRecognizer，长按录音→松手转文字。
 **/
class VoiceRecognizer(private val context: Context) {

    /** 回调接口。[AI生成] */
    interface Callback {
        /** 部分识别结果（实时，非所有设备支持）。[AI生成] */
        fun onPartialResult(text: String)
        /** 最终识别结果。[AI生成] */
        fun onFinalResult(text: String)
        /** 识别出错。[AI生成] */
        fun onError(errorMsg: String)
        /** 用户开始说话（可选，用于 UI 状态）。[AI生成] */
        fun onBeginningOfSpeech() {}
        /** 用户停止说话（可选）。[AI生成] */
        fun onEndOfSpeech() {}
        /** 音量变化（可选，0-10 级别，用于波形动画）。[AI生成] */
        fun onRmsChanged(rmsdB: Float) {}
    }

    private var recognizer: SpeechRecognizer? = null
    private var callback: Callback? = null

    /**
     * 开始语音识别。[AI生成]
     *
     * @param cb 结果回调
     * @return true 启动成功，false 启动失败（设备不支持等）
     */
    fun startListening(cb: Callback): Boolean {
        // 先释放旧的 recognizer（如有）
        destroy()

        callback = cb

        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            AppLogger.w("VoiceRec", "SpeechRecognizer not available on this device")
            cb.onError("此设备不支持语音识别")
            return false
        }

        recognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    AppLogger.d("VoiceRec", "onReadyForSpeech")
                }

                override fun onBeginningOfSpeech() {
                    AppLogger.d("VoiceRec", "onBeginningOfSpeech")
                    callback?.onBeginningOfSpeech()
                }

                override fun onRmsChanged(rmsdB: Float) {
                    callback?.onRmsChanged(rmsdB)
                }

                override fun onBufferReceived(buffer: ByteArray?) {}

                override fun onEndOfSpeech() {
                    AppLogger.d("VoiceRec", "onEndOfSpeech")
                    callback?.onEndOfSpeech()
                }

                override fun onError(error: Int) {
                    val msg = when (error) {
                        SpeechRecognizer.ERROR_AUDIO -> "录音出错"
                        SpeechRecognizer.ERROR_CLIENT -> "客户端出错"
                        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "没有麦克风权限"
                        SpeechRecognizer.ERROR_NETWORK -> "网络连接失败"
                        SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "网络超时"
                        SpeechRecognizer.ERROR_NO_MATCH -> "没有识别到内容"
                        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "语音引擎繁忙"
                        SpeechRecognizer.ERROR_SERVER -> "服务器出错"
                        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "没有检测到说话"
                        else -> "语音识别失败 ($error)"
                    }
                    AppLogger.w("VoiceRec", "recognition_error code=$error")
                    callback?.onError(msg)
                }

                override fun onResults(results: Bundle?) {
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val text = matches?.firstOrNull()?.trim().orEmpty()
                    AppLogger.d("VoiceRec", "final_result has_text=${text.isNotEmpty()}")
                    if (text.isNotEmpty()) {
                        callback?.onFinalResult(text)
                    } else {
                        callback?.onError("没有识别到内容")
                    }
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val text = matches?.firstOrNull()?.trim().orEmpty()
                    if (text.isNotEmpty()) {
                        AppLogger.d("VoiceRec", "partial_result has_text=true")
                        callback?.onPartialResult(text)
                    }
                }

                override fun onEvent(eventType: Int, params: Bundle?) {}
            })

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "zh-CN")
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                // 提示文字（部分设备显示在系统录音对话框上）
                putExtra(RecognizerIntent.EXTRA_PROMPT, "说出你吃了什么…")
            }

            startListening(intent)
        }
        return true
    }

    /** 停止识别并等待最终结果。[AI生成] */
    fun stopListening() {
        recognizer?.stopListening()
    }

    /** 取消识别（不等待结果）。[AI生成] */
    fun cancelListening() {
        recognizer?.cancel()
        callback = null
    }

    /** 释放资源。[AI生成] */
    fun destroy() {
        try {
            recognizer?.cancel()
            recognizer?.destroy()
        } catch (_: Exception) {
            // SpeechRecognizer 在某些状态下 destroy 可能抛异常，忽略
        }
        recognizer = null
        callback = null
    }

    /** 是否正在识别中。[AI生成] */
    val isListening: Boolean get() = recognizer != null
}
