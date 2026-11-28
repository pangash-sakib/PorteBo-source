package com.sakib.portebo.ui;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.sakib.portebo.R;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public final class LanguageManager {

    public static final String ENGLISH = "en";
    public static final String BANGLA = "bn";

    private static final Map<String, String> TO_ENGLISH = new HashMap<>();
    private static final Map<String, String> TO_BANGLA = new HashMap<>();

    static {
        // Developer mode
        add("শুধু অনুমোদিত ডেভেলপার অ্যাকাউন্ট দিয়ে লগইন করো।",
                "Log in with an authorized developer account.");
        add("লগইন", "Log in");
        add("বাতিল", "Cancel");

        // App picker
        add("অ্যাপ বেছে নাও (Greylist-এ যোগ)",
                "Choose apps (add to Greylist)");
        add("হয়ে গেছে", "Done");

        // Exam editor
        add("পরীক্ষা যোগ করো", "Add exam");
        add("সেভ করো", "Save");

        // Guardian
        add("PIN বদলাও", "Change PIN");
        add("এখনই লক করো", "Lock now");
        add("শুধু অভিভাবক অ্যাপের নিয়ম বদলাতে পারবেন।",
                "Only the guardian can change the app rules.");
        add("Guardian PIN দাও", "Enter guardian PIN");
        add("৬–১২ ডিজিটের PIN", "Enter a 6–12 digit PIN");
        add("PIN আবার লিখো", "Enter the PIN again");
        add("Guardian PIN সেট করো", "Set guardian PIN");
        add("সেট করো", "Set");
        add("দুটো PIN মেলেনি", "The PINs do not match");
        add("PIN সেট হয়নি। আবার চেষ্টা করো।",
                "PIN was not set. Please try again.");
        add("Guardian Control চালু হয়েছে।",
                "Guardian Control has been enabled.");
        add("খুলো", "Open");
        add("PIN মেলেনি।", "The PIN is incorrect.");
        add("Guardian session ১৫ মিনিটের জন্য খোলা হয়েছে।",
                "The guardian session is open for 15 minutes.");
        add("এখন তুমি অ্যাপের নিয়ম ও রুটিন পরিবর্তন করতে পারবে।",
                "You can now change the app rules and routine.");
        add("Guardian Control বন্ধ আছে। চালু করলে শিক্ষার্থী নিয়ম বদলাতে পারবে না।",
                "Guardian Control is off. Enabling it prevents the student from changing rules.");
        add("Guardian Control বন্ধ করো", "Turn off Guardian Control");
        add("Guardian Control চালু করো", "Turn on Guardian Control");
        add("Guardian session লক হয়েছে।", "The guardian session has been locked.");
        add("Guardian Control বন্ধ করবে?", "Turn off Guardian Control?");
        add("বন্ধ করলে PIN ছাড়াই সব নিয়ম ও রুটিন বদলানো যাবে।",
                "When disabled, all rules and routines can be changed without a PIN.");
        add("না", "No");
        add("বন্ধ করো", "Turn off");
        add("Guardian Control বন্ধ হয়েছে।",
                "Guardian Control has been turned off.");
        add("নতুন PIN দুবার লিখো。", "Enter the new PIN twice.");
        add("নতুন ৬–১২ ডিজিটের PIN", "New 6–12 digit PIN");
        add("সেভ", "Save");
        add("PIN সেভ হয়নি। আবার চেষ্টা করো।",
                "The PIN was not saved. Please try again.");
        add("Guardian PIN বদলানো হয়েছে।", "Guardian PIN was changed.");
        add("সেশন শেষ। আবার PIN দাও।",
                "The session has ended. Enter the PIN again.");

        // Publish/update dialogs
        add("রিলিজের তথ্য দাও। এরপর APK বেছে নিয়ে প্রকাশ করো।",
                "Enter the release information, then choose and publish the APK.");
        add("টোকেন সেভ হবে না; শুধু এই রিলিজ আপলোডে ব্যবহার হবে।",
                "The token will not be saved; it will only be used for this upload.");
        add("নতুন ভার্সন পাওয়া গেছে", "New version available");
        add("রিলিজ নোট", "Release notes");
        add("পরে", "Later");
        add("এই আপডেটে নতুন কিছু যোগ করা হয়েছে।",
                "New improvements have been added in this update.");
        add("বর্তমান ভার্সন: ", "Current version: ");
        add("আপডেট চেক করো", "Check for updates");
        add("চেক হচ্ছে…", "Checking…");
        add("তুমি আপডেটেড আছো।", "You are up to date.");
        add("৩০ সেকেন্ড পরে আবার পাঠাও।",
                "Please wait 30 seconds before sending again.");
        add("আজ ৫টি মেসেজ পাঠানো হয়েছে।",
                "You have sent 5 messages today.");
        add("ছবি বাছাই খোলা যায়নি।", "Could not open the image picker.");
        add("ছবি যোগ করো (ঐচ্ছিক)", "Add an image (optional)");
        add("ছবি সংযুক্ত · সর্বোচ্চ ২২৫ KB-তে compress হবে",
                "Image attached · It will be compressed to a maximum of 225 KB");
        add("ছবি সরানো হয়েছে।", "Image removed.");
        add("সেটিংস খোলা যায়নি।", "Could not open Settings.");
        add("@pangash.sakib কপি হয়েছে।", "@pangash.sakib was copied.");
        add("ডিবাগ লগ", "Debug log");
        add("লগ কপি হয়েছে।", "Log copied.");
        add("APK রিলিজ প্রকাশ", "Publish APK release");
        add("APK বেছে নাও", "Choose APK");
        add("ভার্সন নম্বর দাও", "Enter the version number");
        add("রিলিজের শিরোনাম দাও", "Enter the release title");
        add("GitHub token দাও", "Enter the GitHub token");
        add("ফাইল বাছাই খোলা যায়নি।", "Could not open the file picker.");
        add("লগআউট হয়েছে।", "Logged out.");
        add("ডেভেলপার টুলস", "Developer tools");
        add("ইনবক্স", "Inbox");
        add("নতুন ভার্সন প্রকাশ", "Publish new version");
        add("লগআউট", "Log out");

        // App tiers
        add("অ্যাপের নিয়ম ঠিক করো", "Manage app rules");
        add("সব", "All");
        add("খোলা", "Open");
        add("টাইমার", "Timer");
        add("ব্লক", "Block");
        add("মেসেজ সেভ", "Save messages");
        add("মেসেজ সেভ হয়েছে।", "Messages saved.");
        add("সব অ্যাপ দেখাও", "Show all apps");
        add("খোলা অ্যাপ দেখাও", "Show open apps");
        add("টাইমার অ্যাপ দেখাও", "Show timer apps");
        add("ব্লক অ্যাপ দেখাও", "Show blocked apps");
        add("অ্যাপ যোগ করুন", "Add app");
        add("বাদ দেওয়া যাবে না", "Cannot be removed");
        add("হোয়াইটলিস্ট", "Whitelist");
        add("গ্রেলিস্ট", "Greylist");
        add("ব্ল্যাকলিস্ট", "Blacklist");
        add("দৈনিক ব্যবহারের কোটা:", "Daily usage quota:");

        // Home
        add("লকডাউন বন্ধ। পড়তে বসলে চালু করো।",
                "Lockdown is off. Turn it on when you start studying.");
        add("ওভারলে পারমিশন: ", "Overlay permission: ");
        add("চালু", "On");
        add("বন্ধ", "Off");
        add("কোনো লকডাউন ঠিক করা নেই। রুটিন সেভ করো।",
                "No lockdown is scheduled. Save a routine.");
        add("পরবর্তী লকডাউন নির্ধারিত নেই।",
                "No next lockdown is scheduled.");
        add("পরবর্তী লকডাউন: ", "Next lockdown: ");
        add("স্টপ স্ক্রিন টেস্ট", "Stop screen test");
        add("অ্যাপ ব্লক স্ক্রিনটি দেখো", "Preview the app block screen");
        add("ইনস্টল করা অ্যাপ", "Installed apps");

        // About screen
        add("পড়তে ব! · পড়ায় মন দাও", "Porte Bo! · Focus on study");
        add("পড়ার সময় ফোনের distraction কমাতে এই অ্যাপ।",
                "This app reduces phone distractions during study time.");
        add("অ্যাপের রং", "App color");
        add("তোমার পছন্দের রং বেছে নাও", "Choose your preferred color");
        add("১. Whitelist · সবসময় খোলা", "1. Whitelist · Always open");
        add("ক্যালকুলেটর, PDF reader, Docs-এর মতো পড়ার অ্যাপ খোলা থাকবে। ফোন, Settings, Messages ও দরকারি system app-ও সবসময় খোলা।",
                "Study apps such as Calculator, PDF reader and Docs will remain open. Phone, Settings, Messages and useful system apps will also remain open.");
        add("২. Greylist · অ্যাপ-পিছু ৫ মিনিট",
                "2. Greylist · 5 minutes per app");
        add("প্রতিটি অ্যাপের আলাদা ৫ মিনিটের কোটা। অ্যাপ ছাড়লে টাইমার থামে, আবার ঢুকলে চলে। স্ক্রিনে ভাসমান টাইমার দেখাবে।",
                "Each app gets a separate 5-minute quota. The timer pauses when you leave the app and resumes when you return. A floating timer appears on the screen.");
        add("৩. Blacklist · লকডাউনে ব্লক",
                "3. Blacklist · Blocked during lockdown");
        add("লকডাউনে এই অ্যাপ খুললে আগে ব্লক স্ক্রিন দেখাবে, তারপর হোমে ফিরবে।",
                "Opening this app during lockdown shows a block screen before returning home.");
        add("পারমিশন গাইড", "Permission guide");
        add("অ্যাপ মনিটর আর ব্লক স্ক্রিন চালাতে Accessibility এবং Display over other apps চালু করো।",
                "Enable Accessibility and Display over other apps to run app monitoring and block screens.");
        add("Accessibility সেটিংস", "Accessibility settings");
        add("ডেভেলপারকে মেসেজ পাঠাও", "Message the developer");
        add("এটা anonymous মেসেজ, কোনো রিপ্লাই পাওয়া যাবে না। তাই যা বলার একসাথে গুছিয়ে লিখে পাঠাও।",
                "This is an anonymous message, so no reply is expected. Write everything you want to say in one message.");
        add("পাঠাও", "Send");
        add("INSTAGRAM: @PANGASH.SAKIB", "INSTAGRAM: @PANGASH.SAKIB");

        // Lockdown overlay
        add("টাইম শেষ। এবার পড়তে বসো।",
                "Time is over. Start studying now.");
        add("এই অ্যাপ লকডাউনে বন্ধ।",
                "This app is disabled during lockdown.");
        add("সিস্টেম অ্যাপ · সবসময় খোলা",
                "System app · Always open");
        add("পড়ার সময়ের অ্যাপ", "Study app");

        // Update process
        add("আপডেট ফাইল নামানো যায়নি।", "Could not download the update file.");
        add("GitHub release-এ APK ফাইল পাওয়া যায়নি।",
                "No APK file was found in the GitHub release.");
        add("GitHub থেকে APK নামানো যায়নি (HTTP ",
                "Could not download the APK from GitHub (HTTP ");
        add("APK-টি PorteBo-এর নয়। সঠিক APK বেছে নাও।",
                "This APK is not for PorteBo. Choose the correct APK.");
        add("APK ভার্সন ইনস্টল করা ভার্সনের চেয়ে নতুন নয়।",
                "The APK version is not newer than the installed version.");
        add("নামানো ফাইলটি সঠিক APK নয়।",
                "The downloaded file is not a valid APK.");
        add("APK প্রস্তুত করা যায়নি। আবার চেষ্টা করো.",
                "Could not prepare the APK. Please try again.");
        add("এই অ্যাপ থেকে APK ইনস্টলের অনুমতি দিয়ে আবার Update চাপো।",
                "Allow APK installation from this app, then press Update again.");
        add("Android installer খোলা হয়েছে। ইনস্টল নিশ্চিত করো।",
                "The Android installer has opened. Confirm the installation.");
        add("রিলিজ প্রকাশ করা যায়নি।", "Could not publish the release.");
        add("নতুন ভার্সন প্রকাশ হয়েছে।", "The new version was published.");
        add("রিলিজ হয়নি। GitHub token, পারমিশন আর APK ফাইল দেখো।",
                "Publishing failed. Check the GitHub token, permissions and APK file.");

        // English strings used directly in layouts.
        add("Report issue", "সমস্যা জানাও");
        add("Suggestion", "পরামর্শ");
        add("Feedback", "মতামত");
        add("See more", "আরও দেখো");
        add("See less", "কম দেখো");
        add("Developer mode login", "ডেভেলপার মোড লগইন");
        add("Display over other apps", "অন্যান্য অ্যাপের উপরে দেখানো");
        add("PORTEBO UPDATE", "PORTEBO আপডেট");
    }

    private LanguageManager() {
    }

    private static void add(String bangla, String english) {
        TO_ENGLISH.put(bangla, english);
        TO_BANGLA.put(english, bangla);
    }

    public static String getLanguage(Context context) {
        if (context == null) {
            return ENGLISH;
        }

        return context.getSharedPreferences("portebo_prefs", Context.MODE_PRIVATE)
                .getString("app_language", ENGLISH);
    }

    public static void setLanguage(Context context, String language) {
        if (context == null || language == null || language.trim().isEmpty()) {
            return;
        }

        context.getSharedPreferences("portebo_prefs", Context.MODE_PRIVATE)
                .edit()
                .putString("app_language", BANGLA.equals(language) ? BANGLA : ENGLISH)
                .apply();
    }

    public static boolean isBangla(Context context) {
        return BANGLA.equals(getLanguage(context));
    }

    public static Context wrap(Context context) {
        Locale locale;

        if (isBangla(context)) {
            locale = new Locale("bn", "BD");
        } else {
            locale = Locale.ENGLISH;
        }

        Resources resources = context.getResources();
        Configuration configuration = new Configuration(resources.getConfiguration());

        if (Build.VERSION.SDK_INT >= 24) {
            configuration.setLocales(new android.os.LocaleList(locale));
        } else {
            configuration.locale = locale;
        }

        configuration.setLayoutDirection(locale);
        return context.createConfigurationContext(configuration);
    }

    public static String text(Context context, String english, String bangla) {
        return isBangla(context) ? bangla : english;
    }

    public static void apply(View view) {
        if (view == null) {
            return;
        }

        Context context = view.getContext();

        if (view instanceof TextView) {
            TextView textView = (TextView) view;
            int id = textView.getId();

            // The top app name and subtitle are intentionally excluded.
            if (id != R.id.tvAppTitle && id != R.id.tvAppSubTag) {
                CharSequence currentText = textView.getText();
                if (currentText != null) {
                    String translated = translate(context, currentText.toString());
                    if (!translated.contentEquals(currentText)) {
                        textView.setText(translated);
                    }
                }

                CharSequence currentHint = textView.getHint();
                if (currentHint != null) {
                    String translatedHint = translate(context, currentHint.toString());
                    if (!translatedHint.contentEquals(currentHint)) {
                        textView.setHint(translatedHint);
                    }
                }
            }

            CharSequence description = textView.getContentDescription();
            if (description != null) {
                String translatedDescription = translate(context, description.toString());
                if (!translatedDescription.contentEquals(description)) {
                    textView.setContentDescription(translatedDescription);
                }
            }
        }

        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;

            for (int i = 0; i < group.getChildCount(); i++) {
                apply(group.getChildAt(i));
            }
        }
    }

    private static String translate(Context context, String source) {
        if (source == null || source.trim().isEmpty()) {
            return source == null ? "" : source;
        }

        boolean bangla = isBangla(context);
        String directTranslation = bangla
                ? TO_BANGLA.get(source)
                : TO_ENGLISH.get(source);

        if (directTranslation != null) {
            return directTranslation;
        }

        if (!bangla) {
            return translateEnglishPatterns(source);
        }

        return source;
    }

    private static String translateEnglishPatterns(String source) {
        if (source.startsWith("লকডাউন চলছে · বাকি ")) {
            return "Lockdown running · "
                    + source.substring("লকডাউন চলছে · বাকি ".length());
        }

        if (source.startsWith("ভার্সন ")) {
            return "Version " + source.substring("ভার্সন ".length());
        }

        if (source.startsWith("সব · ")) {
            return "All · " + source.substring("সব · ".length());
        }

        if (source.startsWith("খোলা · ")) {
            return "Open · " + source.substring("খোলা · ".length());
        }

        if (source.startsWith("টাইমার · ")) {
            return "Timer · " + source.substring("টাইমার · ".length());
        }

        if (source.startsWith("ব্লক · ")) {
            return "Block · " + source.substring("ব্লক · ".length());
        }

        if (source.endsWith(" ব্লক করা হয়েছে")) {
            String appName = source.substring(
                    0,
                    source.length() - " ব্লক করা হয়েছে".length()
            );

            return "Blocked: " + appName;
        }

        if (source.endsWith(" (টাইম শেষ)")) {
            String appName = source.substring(
                    0,
                    source.length() - " (টাইম শেষ)".length()
            );

            return appName + " (time finished)";
        }

        if (source.startsWith("পরবর্তী লকডাউন: ")) {
            return "Next lockdown: "
                    + source.substring("পরবর্তী লকডাউন: ".length())
                    .replace("আজ", "Today")
                    .replace("আগামীকাল", "Tomorrow")
                    .replace("রাত", "night")
                    .replace("ভোর", "early morning")
                    .replace("সকাল", "morning")
                    .replace("দুপুর", "afternoon")
                    .replace("বিকেল", "late afternoon")
                    .replace("সন্ধ্যা", "evening")
                    .replace("মিনিট", "minutes")
                    .replace("ঘণ্টা", "hours")
                    .replace("পরে", "later");
        }

        return source;
    }
}
