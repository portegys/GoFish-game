// Go Fish game sound manager.

package com.dialectek.gofish;

import java.io.IOException;
import java.util.HashMap;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.AudioManager;
import android.media.SoundPool;
import android.util.Log;

public class SoundManager
{
   // Sounds.
   public static final int fanfareSound = 0;
   public static final int winnerSound  = 1;

   private static SoundPool                 mSoundPool;
   private static HashMap<Integer, Integer> mSoundPoolMap;
   private static AudioManager              mAudioManager;
   private static Context mContext;

   private SoundManager()
   {
   }


   /**
    * Initializes the storage for the sounds
    *
    * @param theContext The Application context
    */
   public static void initSounds(Context theContext)
   {
      mContext      = theContext;
      mSoundPool    = new SoundPool(4, AudioManager.STREAM_MUSIC, 0);
      mSoundPoolMap = new HashMap<Integer, Integer>();
      mAudioManager = (AudioManager)mContext.getSystemService(Context.AUDIO_SERVICE);
   }


   /**
    * Add a new Sound to the SoundPool
    *
    * @param Index - The Sound Index for Retrieval
    * @param SoundID - The Android ID for the Sound asset.
    */
   public static void addSound(int Index, int SoundID)
   {
      mSoundPoolMap.put(Index, mSoundPool.load(mContext, SoundID, 1));
   }


   /**
    * Loads the various sound assets
    */
   public static void loadSounds()
   {
      loadSound("fanfare.ogg", fanfareSound);
      loadSound("winner.ogg", winnerSound);
   }


   /**
    * Load a sound
    *
    * @param fileName - The Sound asset file name.
    * @param Index - The Sound Index for Retrieval
    */
   public static void loadSound(String fileName, int Index)
   {
      AssetFileDescriptor assetFd = null;

      try
      {
         assetFd = mContext.getAssets().openFd("sounds/" + fileName);
         mSoundPoolMap.put(Index, mSoundPool.load(assetFd, 1));
      }
      catch (IOException e)
      {
         Log.d("GoFish", "Cannot open sound file: " + fileName);
      }
   }


   /**
    * Plays a Sound
    *
    * @param index - The Index of the Sound to be played
    * @param speed - The Speed to play not, not currently used but included for compatibility
    */
   public static void playSound(int index, float speed)
   {
      float streamVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);

      streamVolume = streamVolume / mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
      Integer id = mSoundPoolMap.get(index);
      if (id != null)
      {
         mSoundPool.play(id, streamVolume, streamVolume, 1, 0, speed);
      }
   }


   /**
    * Stop a Sound
    * @param index - index of the sound to be stopped
    */
   public static void stopSound(int index)
   {
      mSoundPool.stop(mSoundPoolMap.get(index));
   }


   public static void cleanup()
   {
      mSoundPool.release();
      mSoundPool = null;
      mSoundPoolMap.clear();
      mAudioManager.unloadSoundEffects();
   }
}
