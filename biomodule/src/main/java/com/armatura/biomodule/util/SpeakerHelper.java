package com.armatura.biomodule.util;

import android.content.Context;
import android.media.AudioManager;
import android.media.SoundPool;
import android.media.SoundPool.OnLoadCompleteListener;
import android.os.Environment;
import android.util.SparseIntArray;

import java.util.HashMap;
import java.util.Map;


public final class SpeakerHelper {

    @SuppressWarnings("unused")
    /** TAG for logging **/
    private static final String TAG = SpeakerHelper.class.getCanonicalName();

    private static final String DEFAULT_SOUND_PATH = Environment.getExternalStorageDirectory().getPath() + "/ARMATURA/";

    /**
     * rawSounds variable
     **/
    private final static SparseIntArray rawSounds = new SparseIntArray();

    /**
     * externalSounds variable
     **/
    private final static Map<String, Integer> externalSounds = new HashMap<String, Integer>();

    /**
     * listener variable
     **/
    private final static Listener listener = new Listener();

    /**
     * soundPool variable
     **/
    private final static SoundPool soundPool = new SoundPool(8,
            AudioManager.STREAM_MUSIC, 0);

    /**
     * sounds folders
     **/
    private static final String SOUNDS_FOLDER = "sounds/";

    static {
        soundPool.setOnLoadCompleteListener(listener);
    }


	/*
	 * Necessity of utility class
	 */
	private SpeakerHelper() {

	}

	/**
	 * Inner class for Speaker Helper class
	 */
	private static class Listener implements OnLoadCompleteListener {

		@Override
		/** onLoadComplete implementation **/
		public void onLoadComplete(final SoundPool soundPool,
								   final int sampleId, final int status) {
			soundPool.play(sampleId, 1.0f, 1.0f, 1, 0, 1.0f);
		}

		// public static Listener getInstance() {
		// return new Listener();
		// }

	}

	/**
	 * Given a file, generates the full path based on the locale and the gender
	 *
	 * @param context
	 * @param file
	 * @return
	 */
	private static String generateAbsolutePath(final Context context,
											   final String file, final boolean useLocale, final String localName) {
		final StringBuilder builder = new StringBuilder(DEFAULT_SOUND_PATH);
		builder.append(SOUNDS_FOLDER);

		if (useLocale) {
			builder.append(localName);
			builder.append("/");
		}
		builder.append(file);
		return builder.toString();
	}

	/**
	 * Loads the sound into memory
	 *
	 * @param context
	 * @param resId
	 * @return
	 */
	private static int loadSound(final Context context, final int resId) {
		final Integer sound = soundPool.load(context, resId, 1);
		rawSounds.put(resId, sound);
		return sound;
	}

	/**
	 * Loads the external sound into memory
	 *
	 * @param path where the sound is
	 * @return
	 */
	private static int loadSound(final String path) {
		final Integer sound = soundPool.load(path, 1);
		externalSounds.put(path, sound);
		return sound;
	}

	/**
	 * plays the sound. the sound should be in RAW folder
	 *
	 * @param soundID
	 */
	public static void playSound(final Context context, final int soundID) {
		synchronized (soundPool) {
			int sound = rawSounds.get(soundID);
			if (sound == 0) {
				loadSound(context, soundID);
			} else {
				soundPool.play(sound, 1.0f, 1.0f, 1, 0, 1.0f);
			}
		}
	}

	/**
	 * Attempts to load the sound specified from fileName
	 *
	 * @param context
	 * @param fileName  the name of the file
	 * @param useLocale Should be loaded as a locale-specific sound? if yes it will<br>
	 *                  try to load the correct language one and also the gender voice<br>
	 *                  specified in settings
	 */
	public static void playSound(final Context context, final String fileName,
								 final boolean useLocale, final String localName) {
		final String fullPath = generateAbsolutePath(context, fileName, useLocale, localName);
		playSoundWithAbsolutePath(fullPath);
	}

	/**
	 * plays the sound. the sound should be on the external memory. To use local<br>
	 * sounds (languages, and male / female voices) use playSound(Context<br>
	 * context, String fileName, boolean useLocale)
	 */
	public static void playSoundWithAbsolutePath(final String path) {
		synchronized (soundPool) {
			Integer sound = externalSounds.get(path);
			if (sound == null || sound == 0) {
				sound = loadSound(path);
			} else {
				soundPool.play(sound, 1.0f, 1.0f, 1, 0, 1.0f);
			}
		}
	}

}
