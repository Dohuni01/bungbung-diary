package com.bungbung.diary.data.navi

import ai.pleos.playground.navi.data.DestinationInfo
import ai.pleos.playground.navi.data.RouteStartInfo
import ai.pleos.playground.navi.helper.NaviHelper
import ai.pleos.playground.navi.helper.listener.NaviHelperEventListener
import android.content.Context
import android.util.Log

/**
 * Pleos NaviHelper 래핑 — 내비 목적지가 정해지면 이름을 콜백으로 준다.
 *
 * API는 NaviHelper AAR 3.0.5에서 javap으로 확인 (2026-11-11).
 * https://document.pleos.ai/en/api-reference/connect-sdk-pleos/NaviHelper/event-listeners
 *
 * - 목적지 설정·변경: onRoutingCompleted / onDestinationChanged → getDestinationInfo() → onDestinationInfo
 * - 목적지 취소: onRouteCancelled
 * - 초기화 실패 시 조용히 skip (PleosVoiceManager와 같은 원칙) — 디버그 패널 입력이 대신한다.
 * - 같은 목적지로 콜백이 반복돼도 한 번만 알린다.
 */
class PleosNaviDataSource(private val context: Context) {

    private var naviHelper: NaviHelper? = null
    private var listenerAdded = false
    private var lastNotifiedName: String? = null

    /** 목적지 이름이 정해지거나 바뀌었을 때 (이름만 전달 — 좌표·주소는 쓰지 않는다) */
    var onDestinationSet: ((String) -> Unit)? = null

    /** 경로가 취소됐을 때 */
    var onDestinationCleared: (() -> Unit)? = null

    private val listener = object : NaviHelperEventListener {
        // 에뮬레이터 내비는 새 목적지를 정하면 onRouteStarted만 보낸다
        // (onRoutingCompleted/onDestinationChanged는 안 옴 — 2026-11-11 logcat 확인)
        override fun onRouteStarted(routeStartInfo: RouteStartInfo) {
            Log.i(TAG, "onRouteStarted")
            naviHelper?.getDestinationInfo()
        }

        override fun onRoutingCompleted() {
            Log.i(TAG, "onRoutingCompleted")
            naviHelper?.getDestinationInfo()
        }

        override fun onDestinationChanged() {
            Log.i(TAG, "onDestinationChanged")
            naviHelper?.getDestinationInfo()
        }

        override fun onDestinationInfo(destinationInfo: DestinationInfo) {
            val name = destinationInfo.name.trim()
            Log.i(TAG, "onDestinationInfo: $name")
            if (name.isEmpty() || name == lastNotifiedName) return
            lastNotifiedName = name
            onDestinationSet?.invoke(name)
        }

        override fun onRouteCancelled() {
            Log.i(TAG, "onRouteCancelled")
            lastNotifiedName = null
            onDestinationCleared?.invoke()
        }
    }

    fun start() {
        try {
            naviHelper = NaviHelper(context).also { helper ->
                helper.initialize(
                    {
                        helper.addListener(listener)
                        listenerAdded = true
                        // 앱을 켰을 때 이미 경로가 있으면 받아온다
                        helper.getDestinationInfo()
                        Log.i(TAG, "NaviHelper ready")
                    },
                    { throwable -> Log.w(TAG, "NaviHelper init 실패: ${throwable.message}") },
                )
            }
        } catch (e: Exception) {
            Log.w(TAG, "NaviHelper unavailable: ${e.message}")
        }
    }

    fun release() {
        try {
            if (listenerAdded) naviHelper?.removeListener(listener)
            naviHelper?.release()
        } catch (e: Exception) {
            Log.w(TAG, "NaviHelper release 실패: ${e.message}")
        }
        naviHelper = null
        listenerAdded = false
        lastNotifiedName = null
    }

    companion object {
        private const val TAG = "PleosNaviDataSource"
    }
}
