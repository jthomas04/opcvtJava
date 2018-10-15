package studio.v.opcvtjava.auriek;

import android.util.Log;

public class AureikLogger {
    final static int UiThread = 0, AsyncParserThread = 1, RendererThread = 2, CVThread = 3;
    static final String KEY_mode_picked = "mode_picked";
    static final String KEY_AR_state = "AR_state";
    static final String KEY_file_string = "file_string";
    static final String KEY_bitmap_sticker = "bitmap_sticker";

    public static int logIt(String message, int threadId) {
        String t;
        switch (threadId) {
            case UiThread:
                t = "UI_LOG: ";
                break;
            case AsyncParserThread:
                t = "ASYNC_PARSER_LOG: ";
                break;
            case RendererThread:
                t = "RENDERER_LOG: ";
                break;
            case CVThread:
                t = "CV_THREAD";
                break;
            default:
                t = "Unmentioned Thread";
        }
        return Log.i("Aureik Default ", " Aureik " + t + message);
    }

    public static int mLogIt(String message) {
        String t = "Muffle this ";
        return Log.v("Aureik Muffled", "Renderer " + t + message);
        //TODO set your regex to tag:^(?!(Aureik Muffled|<INSERT WHATEVER ELSE TAGS YOU WANT TO IGNORE>))
        //These logs only appear in verbose anyway and are necessary for diagnostic purposes
    }
}
