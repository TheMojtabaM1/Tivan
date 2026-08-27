package ir.tivan.controller.util

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.edit

/**
 * SMS permission handling.
 *
 * The SMS permissions are restricted for apps installed outside a store, so on
 * a sideloaded build the "Allow" option is greyed out and re-requesting can
 * never succeed. The user unlocks it once via App info → ⋮ → "Allow restricted
 * settings" (labelled "More" on Samsung), after which the normal SMS toggle
 * works; [PermissionBanner] walks them through it.
 *
 * The app cannot lift the restriction itself, so rather than looping a dialog
 * that does nothing, it tells the three cases apart and shows the right
 * instructions. Sending keeps working meanwhile by handing the message to the
 * user's own SMS app, which holds the permission itself.
 */
object SmsPermissions {

    val SEND = Manifest.permission.SEND_SMS
    val RECEIVE = Manifest.permission.RECEIVE_SMS
    val READ = Manifest.permission.READ_SMS

    val ALL = arrayOf(SEND, RECEIVE, READ)

    enum class State {
        /** Everything granted; commands send directly and replies parse automatically. */
        Granted,

        /** Not granted yet and the system will still show a dialog. */
        Askable,

        /**
         * Denied with no dialog left to show — either "don't ask again" or the
         * hard restriction on a sideloaded install. Recoverable only from
         * Settings, adb, or by reinstalling through a store.
         */
        Blocked
    }

    private const val PREFS = "sms_permissions"
    private const val KEY_ASKED = "asked"

    fun granted(context: Context, permission: String): Boolean =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

    fun canSendDirectly(context: Context): Boolean = granted(context, SEND)

    /** True when incoming replies can be read, i.e. auto-confirmation works. */
    fun canReceive(context: Context): Boolean = granted(context, RECEIVE)

    fun state(activity: Activity): State {
        val missing = ALL.filterNot { granted(activity, it) }
        if (missing.isEmpty()) return State.Granted

        // shouldShowRationale is false both before the first ask and once the
        // system will no longer prompt, so the "have we asked" flag is what
        // separates "not asked yet" from "asked and permanently refused".
        val anyRationale = missing.any { ActivityCompat.shouldShowRequestPermissionRationale(activity, it) }
        return when {
            anyRationale -> State.Askable
            hasAsked(activity) -> State.Blocked
            else -> State.Askable
        }
    }

    fun hasAsked(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_ASKED, false)

    fun markAsked(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit { putBoolean(KEY_ASKED, true) }
    }

    /** Opens this app's page in Settings, where the user can retry the toggle. */
    fun openAppSettings(context: Context) {
        val intent = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", context.packageName, null)
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(intent) }
    }

    /** The adb command that grants the restricted permissions without a store. */
    fun adbCommand(packageName: String): String =
        "adb shell pm grant $packageName android.permission.RECEIVE_SMS\n" +
            "adb shell pm grant $packageName android.permission.SEND_SMS\n" +
            "adb shell pm grant $packageName android.permission.READ_SMS"
}
