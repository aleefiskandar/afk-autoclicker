package com.example.autoclicker

import android.accessibilityservice.AccessibilityService
import android.graphics.Rect
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class AutoClickService : AccessibilityService() {

    companion object {
        var instance: AutoClickService? = null
        var isAfkActive: Boolean = false
    }

    private var lastClickTimestamp: Long = 0

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (!isAfkActive) return
        
        // Throttle checks to process max once every 600ms to conserve resources
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastClickTimestamp < 600) return

        val rootNode = rootInActiveWindow ?: return
        if (scanAndClickAds(rootNode)) {
            lastClickTimestamp = currentTime
        }
        rootNode.recycle()
    }

    override fun onInterrupt() {
        instance = null
        isAfkActive = false
    }

    private fun scanAndClickAds(node: AccessibilityNodeInfo): Boolean {
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue

            val text = child.text?.toString()?.lowercase() ?: ""
            val contentDesc = child.contentDescription?.toString()?.lowercase() ?: ""
            val viewId = child.viewIdResourceName?.lowercase() ?: ""

            // Comprehensive trigger matrix for ads, close buttons, rewards, and skips
            val isTarget = text.contains("skip") || text.contains("close") || text.contains("no thanks") ||
                    text.contains("claim") || text.contains("reward") || text.contains("collect") ||
                    contentDesc.contains("skip") || contentDesc.contains("close") ||
                    viewId.contains("skip") || viewId.contains("close") || viewId.contains("dismiss")

            if (isTarget) {
                if (child.isClickable) {
                    val clicked = child.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    child.recycle()
                    if (clicked) return true
                } else {
                    // Try to click clickable parent container
                    var parent = child.parent
                    while (parent != null) {
                        if (parent.isClickable) {
                            val clicked = parent.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                            parent.recycle()
                            child.recycle()
                            return true
                        }
                        parent = parent.parent
                    }
                }
            }

            if (scanAndClickAds(child)) {
                child.recycle()
                return true
            }
            child.recycle()
        }
        return false
    }
}
