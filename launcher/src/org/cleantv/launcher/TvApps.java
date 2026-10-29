package org.cleantv.launcher;
import android.content.pm.PackageManager;

/** Installed provider variants across Fire TV, Android TV and Google TV. */
final class TvApps {
    static String[] variants(String pkg) {
        if (pkg.equals("com.amazon.firetv.youtube")) return new String[]{pkg,"com.google.android.youtube.tv"};
        if (pkg.equals("com.amazon.firebat")) return new String[]{pkg,"com.amazon.amazonvideo.livingroom","com.amazon.avod.thirdpartyclient"};
        if (pkg.equals("com.apple.atve.amazon.appletv")) return new String[]{pkg,"com.apple.atve.androidtv.appletv"};
        return new String[]{pkg};
    }
    static String installed(PackageManager pm, String pkg) {
        for (String candidate: variants(pkg))
            if (pm.getLeanbackLaunchIntentForPackage(candidate)!=null || pm.getLaunchIntentForPackage(candidate)!=null) return candidate;
        return pkg;
    }
}
