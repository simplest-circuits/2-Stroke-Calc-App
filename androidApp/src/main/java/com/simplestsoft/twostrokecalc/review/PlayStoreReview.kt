package com.simplestsoft.twostrokecalc.review

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.google.android.play.core.review.ReviewManagerFactory

object PlayStoreReview {
    fun requestReview(activity: Activity) {
        val manager = ReviewManagerFactory.create(activity)
        manager.requestReviewFlow()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    manager.launchReviewFlow(activity, task.result)
                } else {
                    openStoreListing(activity)
                }
            }
    }

    fun openStoreListing(context: Context) {
        val packageName = context.packageName
        val newTaskFlag = if (context is Activity) 0 else Intent.FLAG_ACTIVITY_NEW_TASK
        val marketIntent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("market://details?id=$packageName"),
        ).addFlags(newTaskFlag)
        val webIntent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("https://play.google.com/store/apps/details?id=$packageName"),
        ).addFlags(newTaskFlag)
        runCatching { context.startActivity(marketIntent) }
            .onFailure { runCatching { context.startActivity(webIntent) } }
    }
}
