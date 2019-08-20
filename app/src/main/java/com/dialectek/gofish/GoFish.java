/*
 * Go Fish card game.
 *
 * @(#) GoFish.java	1.0	(tep)	 9/11/2011
 *
 * GoFish
 * Copyright (C) 2011-2012 Tom Portegys
 * All rights reserved.
 *
 * See the file LICENSE.TXT for full copyright and licensing information.
 */

package com.dialectek.gofish;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.UUID;
import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.Log;

public class GoFish extends Activity
{
   private static final String LOG_TAG =
      GoFish.class .getSimpleName();

   // View.
   private GoFishView goFishView;

   // Save file.
   public final String saveFileName = "gofish.txt";

   // Identity.
   private UUID id;

   @Override
   protected void onCreate(Bundle savedInstanceState)
   {
      super.onCreate(savedInstanceState);

      // EULA.
      Eula.show(this);

      // Establish unique identity.
      setID();

      // Create view.
      String savePath = getDir("data", Context.MODE_PRIVATE).getAbsolutePath() + "/" + saveFileName;
      goFishView = new GoFishView(this, savePath, id);
      setContentView(goFishView);
   }


   // Establish unique identity.
   private void setID()
   {
      id = null;
      String fileName = "id.txt";
      String path     = getDir("data", Context.MODE_PRIVATE).getAbsolutePath() + "/" + fileName;
      try
      {
         BufferedReader in = new BufferedReader(new FileReader(path));
         String         s;
         if ((s = in.readLine()) != null)
         {
            id = UUID.fromString(s);
         }
         in.close();
      }
      catch (FileNotFoundException e) {}
      catch (IOException e)
      {
         Log.d("GoFish", "Error reading " + fileName);
      }
      if (id == null)
      {
         try
         {
            PrintWriter out = new PrintWriter(new FileWriter(path));
            id = UUID.randomUUID();
            out.println(id.toString());
            out.close();
         }
         catch (IOException e)
         {
            Log.d("GoFish", "Error writing " + fileName);
         }
      }
   }


   @Override
   public void onConfigurationChanged(Configuration newConfig)
   {
      super.onConfigurationChanged(newConfig);
   }


   @Override
   protected void onPause()
   {
      super.onPause();
      SoundManager.cleanup();
      goFishView.onPause();
   }


   @Override
   protected void onResume()
   {
      super.onResume();
      SoundManager.initSounds(this);
      SoundManager.loadSounds();
      goFishView.onResume();
   }
}
