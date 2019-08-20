// Go Fish game renderer.

package com.dialectek.gofish;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import com.dialectek.gofish.GoFishView.CARD_VISIBILITY;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Typeface;
import android.opengl.GLSurfaceView;
import android.opengl.GLUtils;
import android.os.SystemClock;
import android.util.Log;

public class GoFishRenderer implements GLSurfaceView.Renderer
{
   private static final String LOG_TAG =
      GoFishRenderer.class .getSimpleName();

   // Window dimensions.
   int   windowWidth  = 320;
   int   windowHeight = 480;
   float windowAspect = (float)windowWidth / (float)windowHeight;

   // Context.
   Context context;

   // View.
   GoFishView view;

   // Card proportions.
   public static final float CARD_WIDTH_TO_HEIGHT = 0.739f;

   // Play surface.
   PlaySurface playSurface;

   // Cards.
   final int CLUBS      = 0;
   final int DIAMONDS   = 1;
   final int HEARTS     = 2;
   final int SPADES     = 3;
   final int ACE_CARD   = 0;
   final int TWO_CARD   = 1;
   final int THREE_CARD = 2;
   final int FOUR_CARD  = 3;
   final int FIVE_CARD  = 4;
   final int SIX_CARD   = 5;
   final int SEVEN_CARD = 6;
   final int EIGHT_CARD = 7;
   final int NINE_CARD  = 8;
   final int TEN_CARD   = 9;
   final int JACK_CARD  = 10;
   final int QUEEN_CARD = 11;
   final int KING_CARD  = 12;
   Card[][] cards;
   Card cardBack;
   Card[] numberCards;
   Card Jcard, Qcard, Kcard, Acard;

   // Text.
   LabelMaker labelMaker;
   LabelMaker welcomeLabelMaker;
   int[] charLabels;
   int[] welcomeLabels;
   String[] welcomeText;
   enum CHAR_LABEL_PARMS
   {
      CHAR_OFFSET(32),
      NUM_CHARS(95);
      private int value;
      CHAR_LABEL_PARMS(int value) { this.value = value; }
      int getValue() { return(value); }
   };
   public static final int COLS          = 40;
   public static final int LINES         = 20;
   public static final int MAX_TEXT_SIZE = 32;

   // Reset button.
   public ResetButton resetButton;

   // Drawables valid.
   public boolean drawablesValid;

   // Welcome timing.
   int     WELCOME_TIME = 5000;
   long    welcomeTimer;
   boolean welcome;

   // Constructor.
   public GoFishRenderer(Context context, GoFishView view)
   {
      this.context = context;
      this.view    = view;

      // Initialize state.
      drawablesValid = false;
      welcome        = true;
   }


   // Surface creation.
   @Override
   public void onSurfaceCreated(GL10 gl, EGLConfig config)
   {
      gl.glDisable(GL10.GL_DITHER);
      gl.glHint(GL10.GL_PERSPECTIVE_CORRECTION_HINT,
                GL10.GL_FASTEST);
      gl.glClearColor(0.0f, 1.0f, 0.0f, 1.0f);
      gl.glShadeModel(GL10.GL_SMOOTH);
      gl.glEnable(GL10.GL_DEPTH_TEST);
      gl.glEnable(GL10.GL_TEXTURE_2D);
      gl.glEnable(GL10.GL_CULL_FACE);
      gl.glViewport(0, 0, windowWidth, windowHeight);
   }


   // Reshape.
   @Override
   public void onSurfaceChanged(GL10 gl, int w, int h)
   {
      if ((windowWidth != w) || (windowHeight != h))
      {
         drawablesValid = false;
      }
      windowWidth  = w;
      windowHeight = h;
      windowAspect = (float)windowWidth / (float)windowHeight;
      gl.glViewport(0, 0, w, h);
   }


   // Draw.
   @Override
   public void onDrawFrame(GL10 gl)
   {
      gl.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT);
      gl.glMatrixMode(GL10.GL_PROJECTION);
      gl.glPushMatrix();
      gl.glLoadIdentity();
      gl.glOrthof(0.0f, windowWidth, 0.0f, windowHeight, 0.0f, 1.0f);
      gl.glMatrixMode(GL10.GL_MODELVIEW);
      gl.glPushMatrix();
      gl.glLoadIdentity();

      // Create drawables?
      if (!drawablesValid)
      {
         playSurface = new PlaySurface(this);
         makeCards(gl);
         makeText(gl);
         resetButton = new ResetButton("textures/reset_button.png",
                                       playSurface.resetButtonSize, playSurface.resetButtonSize,
                                       gl, context);
         drawablesValid = true;
      }


      // Welcome?
      if (welcome)
      {
         drawWelcomeText(gl);
         if (SystemClock.uptimeMillis() - welcomeTimer >= WELCOME_TIME)
         {
            welcome = false;
         }
      }
      else
      {
         // Draw play surface.
         synchronized (this)
         {
            playSurface.draw(gl);
         }
      }

      gl.glMatrixMode(GL10.GL_PROJECTION);
      gl.glPopMatrix();
      gl.glMatrixMode(GL10.GL_MODELVIEW);
      gl.glPopMatrix();
      gl.glFlush();
   }


   // Draw text.
   void drawText(GL10 gl, String text, int x, int y)
   {
      labelMaker.beginDrawing(gl, windowWidth, windowHeight);
      char[] textChars = text.toCharArray();
      int[] labels     = new int[textChars.length];
      for (int i = 0; i < textChars.length; i++)
      {
         labels[i] = charLabels[(int)textChars[i] - CHAR_LABEL_PARMS.CHAR_OFFSET.getValue()];
      }
      labelMaker.drawText(gl, labels, x, y);
      labelMaker.endDrawing(gl);
   }


   void drawWelcomeText(GL10 gl)
   {
      welcomeLabelMaker.beginDrawing(gl, windowWidth, windowHeight);
      float offset = welcomeLabelMaker.getHeight(0) * 2.0f;

      for (int i = 0; i < welcomeText.length; i++)
      {
         offset += welcomeLabelMaker.getHeight(i);
         welcomeLabelMaker.draw(gl, 0,
                                (int)((float)windowHeight - offset),
                                welcomeLabels[i]);
      }
      welcomeLabelMaker.endDrawing(gl);
   }


   // Make cards.
   public void makeCards(GL10 gl)
   {
      // Create card drawables.
      cards = new Card[4][13];
      cards[CLUBS][ACE_CARD] = new Card("textures/cards/c1.png",
                                        playSurface.cardWidth, playSurface.cardHeight,
                                        gl, context);
      cards[CLUBS][TWO_CARD] = new Card("textures/cards/c2.png",
                                        playSurface.cardWidth, playSurface.cardHeight,
                                        gl, context);
      cards[CLUBS][THREE_CARD] = new Card("textures/cards/c3.png",
                                          playSurface.cardWidth, playSurface.cardHeight,
                                          gl, context);
      cards[CLUBS][FOUR_CARD] = new Card("textures/cards/c4.png",
                                         playSurface.cardWidth, playSurface.cardHeight,
                                         gl, context);
      cards[CLUBS][FIVE_CARD] = new Card("textures/cards/c5.png",
                                         playSurface.cardWidth, playSurface.cardHeight,
                                         gl, context);
      cards[CLUBS][SIX_CARD] = new Card("textures/cards/c6.png",
                                        playSurface.cardWidth, playSurface.cardHeight,
                                        gl, context);
      cards[CLUBS][SEVEN_CARD] = new Card("textures/cards/c7.png",
                                          playSurface.cardWidth, playSurface.cardHeight,
                                          gl, context);
      cards[CLUBS][EIGHT_CARD] = new Card("textures/cards/c8.png",
                                          playSurface.cardWidth, playSurface.cardHeight,
                                          gl, context);
      cards[CLUBS][NINE_CARD] = new Card("textures/cards/c9.png",
                                         playSurface.cardWidth, playSurface.cardHeight,
                                         gl, context);
      cards[CLUBS][TEN_CARD] = new Card("textures/cards/c10.png",
                                        playSurface.cardWidth, playSurface.cardHeight,
                                        gl, context);
      cards[CLUBS][JACK_CARD] = new Card("textures/cards/cj.png",
                                         playSurface.cardWidth, playSurface.cardHeight,
                                         gl, context);
      cards[CLUBS][QUEEN_CARD] = new Card("textures/cards/cq.png",
                                          playSurface.cardWidth, playSurface.cardHeight,
                                          gl, context);
      cards[CLUBS][KING_CARD] = new Card("textures/cards/ck.png",
                                         playSurface.cardWidth, playSurface.cardHeight,
                                         gl, context);
      cards[DIAMONDS][ACE_CARD] = new Card("textures/cards/d1.png",
                                           playSurface.cardWidth, playSurface.cardHeight,
                                           gl, context);
      cards[DIAMONDS][TWO_CARD] = new Card("textures/cards/d2.png",
                                           playSurface.cardWidth, playSurface.cardHeight,
                                           gl, context);
      cards[DIAMONDS][THREE_CARD] = new Card("textures/cards/d3.png",
                                             playSurface.cardWidth, playSurface.cardHeight,
                                             gl, context);
      cards[DIAMONDS][FOUR_CARD] = new Card("textures/cards/d4.png",
                                            playSurface.cardWidth, playSurface.cardHeight,
                                            gl, context);
      cards[DIAMONDS][FIVE_CARD] = new Card("textures/cards/d5.png",
                                            playSurface.cardWidth, playSurface.cardHeight,
                                            gl, context);
      cards[DIAMONDS][SIX_CARD] = new Card("textures/cards/d6.png",
                                           playSurface.cardWidth, playSurface.cardHeight,
                                           gl, context);
      cards[DIAMONDS][SEVEN_CARD] = new Card("textures/cards/d7.png",
                                             playSurface.cardWidth, playSurface.cardHeight,
                                             gl, context);
      cards[DIAMONDS][EIGHT_CARD] = new Card("textures/cards/d8.png",
                                             playSurface.cardWidth, playSurface.cardHeight,
                                             gl, context);
      cards[DIAMONDS][NINE_CARD] = new Card("textures/cards/d9.png",
                                            playSurface.cardWidth, playSurface.cardHeight,
                                            gl, context);
      cards[DIAMONDS][TEN_CARD] = new Card("textures/cards/d10.png",
                                           playSurface.cardWidth, playSurface.cardHeight,
                                           gl, context);
      cards[DIAMONDS][JACK_CARD] = new Card("textures/cards/dj.png",
                                            playSurface.cardWidth, playSurface.cardHeight,
                                            gl, context);
      cards[DIAMONDS][QUEEN_CARD] = new Card("textures/cards/dq.png",
                                             playSurface.cardWidth, playSurface.cardHeight,
                                             gl, context);
      cards[DIAMONDS][KING_CARD] = new Card("textures/cards/dk.png",
                                            playSurface.cardWidth, playSurface.cardHeight,
                                            gl, context);
      cards[HEARTS][ACE_CARD] = new Card("textures/cards/h1.png",
                                         playSurface.cardWidth, playSurface.cardHeight,
                                         gl, context);
      cards[HEARTS][TWO_CARD] = new Card("textures/cards/h2.png",
                                         playSurface.cardWidth, playSurface.cardHeight,
                                         gl, context);
      cards[HEARTS][THREE_CARD] = new Card("textures/cards/h3.png",
                                           playSurface.cardWidth, playSurface.cardHeight,
                                           gl, context);
      cards[HEARTS][FOUR_CARD] = new Card("textures/cards/h4.png",
                                          playSurface.cardWidth, playSurface.cardHeight,
                                          gl, context);
      cards[HEARTS][FIVE_CARD] = new Card("textures/cards/h5.png",
                                          playSurface.cardWidth, playSurface.cardHeight,
                                          gl, context);
      cards[HEARTS][SIX_CARD] = new Card("textures/cards/h6.png",
                                         playSurface.cardWidth, playSurface.cardHeight,
                                         gl, context);
      cards[HEARTS][SEVEN_CARD] = new Card("textures/cards/h7.png",
                                           playSurface.cardWidth, playSurface.cardHeight,
                                           gl, context);
      cards[HEARTS][EIGHT_CARD] = new Card("textures/cards/h8.png",
                                           playSurface.cardWidth, playSurface.cardHeight,
                                           gl, context);
      cards[HEARTS][NINE_CARD] = new Card("textures/cards/h9.png",
                                          playSurface.cardWidth, playSurface.cardHeight,
                                          gl, context);
      cards[HEARTS][TEN_CARD] = new Card("textures/cards/h10.png",
                                         playSurface.cardWidth, playSurface.cardHeight,
                                         gl, context);
      cards[HEARTS][JACK_CARD] = new Card("textures/cards/hj.png",
                                          playSurface.cardWidth, playSurface.cardHeight,
                                          gl, context);
      cards[HEARTS][QUEEN_CARD] = new Card("textures/cards/hq.png",
                                           playSurface.cardWidth, playSurface.cardHeight,
                                           gl, context);
      cards[HEARTS][KING_CARD] = new Card("textures/cards/hk.png",
                                          playSurface.cardWidth, playSurface.cardHeight,
                                          gl, context);
      cards[SPADES][ACE_CARD] = new Card("textures/cards/s1.png",
                                         playSurface.cardWidth, playSurface.cardHeight,
                                         gl, context);
      cards[SPADES][TWO_CARD] = new Card("textures/cards/s2.png",
                                         playSurface.cardWidth, playSurface.cardHeight,
                                         gl, context);
      cards[SPADES][THREE_CARD] = new Card("textures/cards/s3.png",
                                           playSurface.cardWidth, playSurface.cardHeight,
                                           gl, context);
      cards[SPADES][FOUR_CARD] = new Card("textures/cards/s4.png",
                                          playSurface.cardWidth, playSurface.cardHeight,
                                          gl, context);
      cards[SPADES][FIVE_CARD] = new Card("textures/cards/s5.png",
                                          playSurface.cardWidth, playSurface.cardHeight,
                                          gl, context);
      cards[SPADES][SIX_CARD] = new Card("textures/cards/s6.png",
                                         playSurface.cardWidth, playSurface.cardHeight,
                                         gl, context);
      cards[SPADES][SEVEN_CARD] = new Card("textures/cards/s7.png",
                                           playSurface.cardWidth, playSurface.cardHeight,
                                           gl, context);
      cards[SPADES][EIGHT_CARD] = new Card("textures/cards/s8.png",
                                           playSurface.cardWidth, playSurface.cardHeight,
                                           gl, context);
      cards[SPADES][NINE_CARD] = new Card("textures/cards/s9.png",
                                          playSurface.cardWidth, playSurface.cardHeight,
                                          gl, context);
      cards[SPADES][TEN_CARD] = new Card("textures/cards/s10.png",
                                         playSurface.cardWidth, playSurface.cardHeight,
                                         gl, context);
      cards[SPADES][JACK_CARD] = new Card("textures/cards/sj.png",
                                          playSurface.cardWidth, playSurface.cardHeight,
                                          gl, context);
      cards[SPADES][QUEEN_CARD] = new Card("textures/cards/sq.png",
                                           playSurface.cardWidth, playSurface.cardHeight,
                                           gl, context);
      cards[SPADES][KING_CARD] = new Card("textures/cards/sk.png",
                                          playSurface.cardWidth, playSurface.cardHeight,
                                          gl, context);
      cardBack = new Card("textures/cards/b2fv.png",
                          playSurface.cardWidth, playSurface.cardHeight,
                          gl, context);
      numberCards    = new Card[14];
      numberCards[0] = new Card("textures/cards/0.png",
                                playSurface.cardWidth, playSurface.cardHeight,
                                gl, context);
      numberCards[1] = new Card("textures/cards/1.png",
                                playSurface.cardWidth, playSurface.cardHeight,
                                gl, context);
      numberCards[2] = new Card("textures/cards/2.png",
                                playSurface.cardWidth, playSurface.cardHeight,
                                gl, context);
      numberCards[3] = new Card("textures/cards/3.png",
                                playSurface.cardWidth, playSurface.cardHeight,
                                gl, context);
      numberCards[4] = new Card("textures/cards/4.png",
                                playSurface.cardWidth, playSurface.cardHeight,
                                gl, context);
      numberCards[5] = new Card("textures/cards/5.png",
                                playSurface.cardWidth, playSurface.cardHeight,
                                gl, context);
      numberCards[6] = new Card("textures/cards/6.png",
                                playSurface.cardWidth, playSurface.cardHeight,
                                gl, context);
      numberCards[7] = new Card("textures/cards/7.png",
                                playSurface.cardWidth, playSurface.cardHeight,
                                gl, context);
      numberCards[8] = new Card("textures/cards/8.png",
                                playSurface.cardWidth, playSurface.cardHeight,
                                gl, context);
      numberCards[9] = new Card("textures/cards/9.png",
                                playSurface.cardWidth, playSurface.cardHeight,
                                gl, context);
      numberCards[10] = new Card("textures/cards/10.png",
                                 playSurface.cardWidth, playSurface.cardHeight,
                                 gl, context);
      numberCards[11] = new Card("textures/cards/11.png",
                                 playSurface.cardWidth, playSurface.cardHeight,
                                 gl, context);
      numberCards[12] = new Card("textures/cards/12.png",
                                 playSurface.cardWidth, playSurface.cardHeight,
                                 gl, context);
      numberCards[13] = new Card("textures/cards/13.png",
                                 playSurface.cardWidth, playSurface.cardHeight,
                                 gl, context);
      Jcard = new Card("textures/cards/J.png",
                       playSurface.cardWidth, playSurface.cardHeight,
                       gl, context);
      Qcard = new Card("textures/cards/Q.png",
                       playSurface.cardWidth, playSurface.cardHeight,
                       gl, context);
      Kcard = new Card("textures/cards/K.png",
                       playSurface.cardWidth, playSurface.cardHeight,
                       gl, context);
      Acard = new Card("textures/cards/A.png",
                       playSurface.cardWidth, playSurface.cardHeight,
                       gl, context);
   }


   // Make text.
   public void makeText(GL10 gl)
   {
      int i;

      // Create small text labels.
      if (labelMaker != null)
      {
         labelMaker.shutdown(gl);
      }
      int textSize = Math.min((windowWidth / COLS), (windowHeight / LINES));

      if (context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT)
      {
         textSize = (int)((float)textSize * 1.9f);
      }
      else
      {
         textSize = (int)((float)textSize * 1.5f);
      }
      if (textSize > MAX_TEXT_SIZE) { textSize = MAX_TEXT_SIZE; }
      int x = CHAR_LABEL_PARMS.NUM_CHARS.getValue() * textSize * 2;
      int y = textSize * 2;
      int w2, h2;
      for (w2 = 2; w2 < x; w2 *= 2) {}
      for (h2 = 2; h2 < y; h2 *= 2) {}
      labelMaker = new LabelMaker(true, w2, h2);
      labelMaker.initialize(gl);
      labelMaker.beginAdding(gl);
      Paint labelPaint = new Paint();
      labelPaint.setTypeface(Typeface.MONOSPACE);
      labelPaint.setTextSize(textSize);
      labelPaint.setAntiAlias(true);
      labelPaint.setARGB(255, 0, 0, 0);
      charLabels = new int[CHAR_LABEL_PARMS.NUM_CHARS.getValue()];
      for (i = 0; i < charLabels.length; i++)
      {
         charLabels[i] =
            labelMaker.add(gl,
                           Character.toString((char)(i + CHAR_LABEL_PARMS.CHAR_OFFSET.getValue())),
                           labelPaint);
      }
      labelMaker.endAdding(gl);

      // Create welcome text.
      if (welcomeLabelMaker != null)
      {
         welcomeLabelMaker.shutdown(gl);
      }
      welcomeText    = new String[3];
      welcomeText[0] = "Welcome to Go Fish";
      welcomeText[1] = "dialectek.com/gofish";
      welcomeText[2] = "Use arrows to play";
      textSize       = windowWidth / welcomeText[0].length();
      if (textSize > MAX_TEXT_SIZE) { textSize = MAX_TEXT_SIZE; }
      x = welcomeText[0].length() * textSize * 2;
      y = textSize * 2;
      for (w2 = 2; w2 < x; w2 *= 2) {}
      for (h2 = 2; h2 < y; h2 *= 2) {}
      welcomeLabelMaker = new LabelMaker(true, w2, h2);
      welcomeLabelMaker.initialize(gl);
      welcomeLabelMaker.beginAdding(gl);
      labelPaint = new Paint();
      labelPaint.setTypeface(Typeface.MONOSPACE);
      labelPaint.setTextSize(textSize);
      labelPaint.setAntiAlias(true);
      labelPaint.setARGB(255, 0, 0, 0);
      welcomeLabels = new int[welcomeText.length];
      for (i = 0; i < welcomeText.length; i++)
      {
         welcomeLabels[i] = welcomeLabelMaker.add(gl, welcomeText[i], labelPaint);
      }
      welcomeLabelMaker.endAdding(gl);
   }


   // Play surface.
   class PlaySurface
   {
      public GoFishRenderer renderer;
      public GoFishView     view;

      // Sizes.
      public float cardWidth, cardHeight;
      public float cardOffset;
      public float myCardHandSpacing;
      public float cardColSpacing, cardRowSpacing;
      public float arrowSize;
      public float resetButtonSize;

      // Layout.
      public       Point[] myCardsHolder;
      public Point otherHand;
      public Point myScoreCard, otherScoreCard;
      public Point myStageCard, otherStageCard;
      public Point exchangeCard, drawCard;
      public Point myScoreToStageBegin, myScoreToStageEnd;
      public Point otherScoreToStageBegin, otherScoreToStageEnd;
      public Point exchangeToDrawBegin, exchangeToDrawEnd;
      public Point myHandToStageBegin, myHandToStageEnd;
      public Point otherHandToStageBegin, otherHandToStageEnd;
      public Point myStageToExchangeBegin, myStageToExchangeEnd;
      public Point otherStageToExchangeBegin, otherStageToExchangeEnd;
      public Point myHandLeftArrow;
      public Point myHandRightArrow;
      public Point myScoreToStageArrow;
      public Point otherScoreToStageArrow;
      public Point exchangeToDrawArrow;
      public Point myHandToStageArrow;
      public Point otherHandToStageArrow;
      public Point myStageToExchangeArrow;
      public Point otherStageToExchangeArrow;
      public Point resetButtonDraw;
      public Point resetButtonTouch;

      // Drawables.
      private CardFrame   cardFrame;
      private Arrow       arrow;
      private FloatBuffer myScoreToStageLine;
      private FloatBuffer otherScoreToStageLine;
      private FloatBuffer exchangeToDrawLine;
      private FloatBuffer myHandToStageLine;
      private FloatBuffer otherHandToStageLine;
      private FloatBuffer myStageToExchangeLine;
      private FloatBuffer otherStageToExchangeLine;

      public PlaySurface(GoFishRenderer renderer)
      {
         this.renderer = renderer;
         view          = renderer.view;

         // Calculate sizes.
         cardOffset = 0.2f;
         cardWidth  = (float)renderer.windowWidth /
                      (13.0f + (14.0f * 0.4f));
         cardHeight = (float)renderer.windowHeight /
                      (5.0f + (6.0f * 0.5f));
         float h2 = cardWidth / CARD_WIDTH_TO_HEIGHT;
         float w2 = cardHeight * CARD_WIDTH_TO_HEIGHT;
         if (h2 < cardHeight)
         {
            cardHeight = h2;
         }
         else if (w2 < cardWidth)
         {
            cardWidth = w2;
         }
         cardOffset       *= cardWidth;
         myCardHandSpacing = ((float)windowWidth -
                              (cardWidth * 13.0f)) / 14.0f;
         cardColSpacing = ((float)windowWidth -
                           (cardWidth * 3.0f)) / 4.0f;
         cardRowSpacing = ((float)windowHeight -
                           (cardHeight * 5.0f)) / 6.0f;
         arrowSize       = cardWidth / 2.0f;
         resetButtonSize = cardWidth * 2.0f;

         // Layout surface.
         myCardsHolder = new Point[13];
         for (int i = 0; i < 13; i++)
         {
            myCardsHolder[i]   = new Point();
            myCardsHolder[i].x = (int)(((cardWidth + myCardHandSpacing) * (float)i) + myCardHandSpacing);
            myCardsHolder[i].y = (int)cardRowSpacing;
         }
         otherHand   = new Point();
         otherHand.x = (int)(((float)windowWidth / 2.0f) -
                             ((float)cardWidth / 2.0f));
         otherHand.y = (int)((cardHeight * 4.0f) +
                             (cardRowSpacing * 5.0f));
         myScoreCard   = new Point();
         myScoreCard.x = (int)cardColSpacing;
         myScoreCard.y = (int)(cardHeight +
                               (cardRowSpacing * 2.0f));
         otherScoreCard   = new Point();
         otherScoreCard.x = (int)cardColSpacing;
         otherScoreCard.y = (int)((cardHeight * 3.0f) +
                                  (cardRowSpacing * 4.0f));
         myStageCard   = new Point();
         myStageCard.x = (int)((cardColSpacing * 2.0f) +
                               cardWidth);
         myStageCard.y = (int)(cardHeight +
                               (cardRowSpacing * 2.0f));
         otherStageCard   = new Point();
         otherStageCard.x = (int)((cardColSpacing * 2.0f) +
                                  cardWidth);
         otherStageCard.y = (int)((cardHeight * 3.0f) +
                                  (cardRowSpacing * 4.0f));
         exchangeCard   = new Point();
         exchangeCard.x = (int)((cardColSpacing * 2.0f) +
                                cardWidth);
         exchangeCard.y = (int)((cardHeight * 2.0f) +
                                (cardRowSpacing * 3.0f));
         drawCard   = new Point();
         drawCard.x = (int)((cardColSpacing * 3.0f) +
                            (cardWidth * 2.0f));
         drawCard.y = (int)((cardHeight * 2.0f) +
                            (cardRowSpacing * 3.0f));
         myScoreToStageBegin         = new Point();
         myScoreToStageBegin.x       = myScoreCard.x + (int)cardWidth;
         myScoreToStageBegin.y       = myScoreCard.y + (int)(cardHeight / 2.0f);
         myScoreToStageEnd           = new Point();
         myScoreToStageEnd.x         = myStageCard.x;
         myScoreToStageEnd.y         = myScoreToStageBegin.y;
         otherScoreToStageBegin      = new Point();
         otherScoreToStageBegin.x    = otherScoreCard.x + (int)cardWidth;
         otherScoreToStageBegin.y    = otherScoreCard.y + (int)(cardHeight / 2.0f);
         otherScoreToStageEnd        = new Point();
         otherScoreToStageEnd.x      = otherStageCard.x;
         otherScoreToStageEnd.y      = otherScoreToStageBegin.y;
         exchangeToDrawBegin         = new Point();
         exchangeToDrawBegin.x       = exchangeCard.x + (int)cardWidth;
         exchangeToDrawBegin.y       = exchangeCard.y + (int)(cardHeight / 2.0f);
         exchangeToDrawEnd           = new Point();
         exchangeToDrawEnd.x         = drawCard.x;
         exchangeToDrawEnd.y         = exchangeToDrawBegin.y;
         myHandToStageBegin          = new Point();
         myHandToStageBegin.x        = myStageCard.x + (int)(cardWidth / 2.0f);
         myHandToStageBegin.y        = myCardsHolder[0].y + (int)cardHeight;
         myHandToStageEnd            = new Point();
         myHandToStageEnd.x          = myHandToStageBegin.x;
         myHandToStageEnd.y          = myStageCard.y;
         otherHandToStageBegin       = new Point();
         otherHandToStageBegin.x     = otherStageCard.x + (int)(cardWidth / 2.0f);
         otherHandToStageBegin.y     = otherHand.y;
         otherHandToStageEnd         = new Point();
         otherHandToStageEnd.x       = otherHandToStageBegin.x;
         otherHandToStageEnd.y       = otherStageCard.y + (int)cardHeight;
         myStageToExchangeBegin      = new Point();
         myStageToExchangeBegin.x    = myStageCard.x + (int)(cardWidth / 2.0f);
         myStageToExchangeBegin.y    = myStageCard.y + (int)cardHeight;
         myStageToExchangeEnd        = new Point();
         myStageToExchangeEnd.x      = myStageToExchangeBegin.x;
         myStageToExchangeEnd.y      = exchangeCard.y;
         otherStageToExchangeBegin   = new Point();
         otherStageToExchangeBegin.x = otherStageCard.x + (int)(cardWidth / 2.0f);
         otherStageToExchangeBegin.y = otherStageCard.y;
         otherStageToExchangeEnd     = new Point();
         otherStageToExchangeEnd.x   = otherStageToExchangeBegin.x;
         otherStageToExchangeEnd.y   = exchangeCard.y + (int)cardHeight;
         myHandLeftArrow             = new Point();
         myHandLeftArrow.x           = (int)((float)windowWidth / 3.0f);
         myHandLeftArrow.y           = (int)((float)(myHandToStageBegin.y +
                                                     myHandToStageEnd.y) / 2.0f);
         myHandRightArrow   = new Point();
         myHandRightArrow.x = (int)((float)windowWidth * (2.0f / 3.0f));
         myHandRightArrow.y = (int)((float)(myHandToStageBegin.y +
                                            myHandToStageEnd.y) / 2.0f);
         myScoreToStageArrow   = new Point();
         myScoreToStageArrow.x = (int)((float)(myScoreToStageBegin.x +
                                               myScoreToStageEnd.x) / 2.0f);
         myScoreToStageArrow.y    = myScoreToStageBegin.y;
         otherScoreToStageArrow   = new Point();
         otherScoreToStageArrow.x = (int)((float)(otherScoreToStageBegin.x +
                                                  otherScoreToStageEnd.x) / 2.0f);
         otherScoreToStageArrow.y = otherScoreToStageBegin.y;
         exchangeToDrawArrow      = new Point();
         exchangeToDrawArrow.x    = (int)((float)(exchangeToDrawBegin.x +
                                                  exchangeToDrawEnd.x) / 2.0f);
         exchangeToDrawArrow.y = exchangeToDrawBegin.y;
         myHandToStageArrow    = new Point();
         myHandToStageArrow.x  = myHandToStageBegin.x;
         myHandToStageArrow.y  = (int)((float)(myHandToStageBegin.y +
                                               myHandToStageEnd.y) / 2.0f);
         otherHandToStageArrow   = new Point();
         otherHandToStageArrow.x = otherHandToStageBegin.x;
         otherHandToStageArrow.y = (int)((float)(otherHandToStageBegin.y +
                                                 otherHandToStageEnd.y) / 2.0f);
         myStageToExchangeArrow   = new Point();
         myStageToExchangeArrow.x = myStageToExchangeBegin.x;
         myStageToExchangeArrow.y = (int)((float)(myStageToExchangeBegin.y +
                                                  myStageToExchangeEnd.y) / 2.0f);
         otherStageToExchangeArrow   = new Point();
         otherStageToExchangeArrow.x = otherStageToExchangeBegin.x;
         otherStageToExchangeArrow.y = (int)((float)(otherStageToExchangeBegin.y +
                                                     otherStageToExchangeEnd.y) / 2.0f);
         resetButtonDraw    = new Point();
         resetButtonDraw.x  = (int)((float)windowWidth - resetButtonSize);
         resetButtonDraw.y  = (int)((float)windowHeight - resetButtonSize);
         resetButtonTouch   = new Point();
         resetButtonTouch.x = (int)((float)resetButtonDraw.x + (resetButtonSize / 2.0f));
         resetButtonTouch.y = (int)((float)resetButtonDraw.y + (resetButtonSize / 2.0f));

         // Create drawables.
         cardFrame = new CardFrame(cardWidth, cardHeight);
         arrow     = new Arrow(arrowSize);
         ByteBuffer vbb = ByteBuffer.allocateDirect(2 * 2 * 4);
         vbb.order(ByteOrder.nativeOrder());
         myScoreToStageLine = vbb.asFloatBuffer();
         myScoreToStageLine.put(myScoreToStageBegin.x);
         myScoreToStageLine.put(myScoreToStageBegin.y);
         myScoreToStageLine.put(myScoreToStageEnd.x);
         myScoreToStageLine.put(myScoreToStageEnd.y);
         myScoreToStageLine.position(0);
         vbb = ByteBuffer.allocateDirect(2 * 2 * 4);
         vbb.order(ByteOrder.nativeOrder());
         otherScoreToStageLine = vbb.asFloatBuffer();
         otherScoreToStageLine.put(otherScoreToStageBegin.x);
         otherScoreToStageLine.put(otherScoreToStageBegin.y);
         otherScoreToStageLine.put(otherScoreToStageEnd.x);
         otherScoreToStageLine.put(otherScoreToStageEnd.y);
         otherScoreToStageLine.position(0);
         vbb = ByteBuffer.allocateDirect(2 * 2 * 4);
         vbb.order(ByteOrder.nativeOrder());
         exchangeToDrawLine = vbb.asFloatBuffer();
         exchangeToDrawLine.put(exchangeToDrawBegin.x);
         exchangeToDrawLine.put(exchangeToDrawBegin.y);
         exchangeToDrawLine.put(exchangeToDrawEnd.x);
         exchangeToDrawLine.put(exchangeToDrawEnd.y);
         exchangeToDrawLine.position(0);
         vbb = ByteBuffer.allocateDirect(2 * 2 * 4);
         vbb.order(ByteOrder.nativeOrder());
         myHandToStageLine = vbb.asFloatBuffer();
         myHandToStageLine.put(myHandToStageBegin.x);
         myHandToStageLine.put(myHandToStageBegin.y);
         myHandToStageLine.put(myHandToStageEnd.x);
         myHandToStageLine.put(myHandToStageEnd.y);
         myHandToStageLine.position(0);
         vbb = ByteBuffer.allocateDirect(2 * 2 * 4);
         vbb.order(ByteOrder.nativeOrder());
         otherHandToStageLine = vbb.asFloatBuffer();
         otherHandToStageLine.put(otherHandToStageBegin.x);
         otherHandToStageLine.put(otherHandToStageBegin.y);
         otherHandToStageLine.put(otherHandToStageEnd.x);
         otherHandToStageLine.put(otherHandToStageEnd.y);
         otherHandToStageLine.position(0);
         vbb = ByteBuffer.allocateDirect(2 * 2 * 4);
         vbb.order(ByteOrder.nativeOrder());
         myStageToExchangeLine = vbb.asFloatBuffer();
         myStageToExchangeLine.put(myStageToExchangeBegin.x);
         myStageToExchangeLine.put(myStageToExchangeBegin.y);
         myStageToExchangeLine.put(myStageToExchangeEnd.x);
         myStageToExchangeLine.put(myStageToExchangeEnd.y);
         myStageToExchangeLine.position(0);
         vbb = ByteBuffer.allocateDirect(2 * 2 * 4);
         vbb.order(ByteOrder.nativeOrder());
         otherStageToExchangeLine = vbb.asFloatBuffer();
         otherStageToExchangeLine.put(otherStageToExchangeBegin.x);
         otherStageToExchangeLine.put(otherStageToExchangeBegin.y);
         otherStageToExchangeLine.put(otherStageToExchangeEnd.x);
         otherStageToExchangeLine.put(otherStageToExchangeEnd.y);
         otherStageToExchangeLine.position(0);
      }


      public void draw(GL10 gl)
      {
         // Draw layout.
         gl.glDisable(GL10.GL_TEXTURE_2D);
         gl.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
         gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
         gl.glVertexPointer(2, GL10.GL_FLOAT, 0, myScoreToStageLine);
         gl.glDrawArrays(GL10.GL_LINES, 0, 2);
         gl.glVertexPointer(2, GL10.GL_FLOAT, 0, otherScoreToStageLine);
         gl.glDrawArrays(GL10.GL_LINES, 0, 2);
         gl.glVertexPointer(2, GL10.GL_FLOAT, 0, exchangeToDrawLine);
         gl.glDrawArrays(GL10.GL_LINES, 0, 2);
         gl.glVertexPointer(2, GL10.GL_FLOAT, 0, myHandToStageLine);
         gl.glDrawArrays(GL10.GL_LINES, 0, 2);
         gl.glVertexPointer(2, GL10.GL_FLOAT, 0, otherHandToStageLine);
         gl.glDrawArrays(GL10.GL_LINES, 0, 2);
         gl.glVertexPointer(2, GL10.GL_FLOAT, 0, myStageToExchangeLine);
         gl.glDrawArrays(GL10.GL_LINES, 0, 2);
         gl.glVertexPointer(2, GL10.GL_FLOAT, 0, otherStageToExchangeLine);
         gl.glDrawArrays(GL10.GL_LINES, 0, 2);
         gl.glDisableClientState(GL10.GL_VERTEX_ARRAY);

         // Draw cards.
         int   i, j, k;
         float offset;
         for (i = 0; i < 13; i++)
         {
            j = i - view.myHandShift;
            while (j < 0) { j += 13; }
            j %= 13;
            if (view.myHand[j].visibility != CARD_VISIBILITY.HIDDEN)
            {
               offset = (float)view.myHand[j].getCount() * cardOffset;
               if (offset > 0.0f)
               {
                  for (k = 3; k >= 0; k--)
                  {
                     if (view.myHand[j].cards[k] != null)
                     {
                        gl.glPushMatrix();
                        offset -= cardOffset;
                        gl.glTranslatef((int)((float)myCardsHolder[i].x + offset), (myCardsHolder[i].y - offset), 0.0f);
                        cards[k][j].draw(gl);
                        gl.glPopMatrix();
                     }
                  }
               }
               else
               {
                  gl.glPushMatrix();
                  gl.glTranslatef(myCardsHolder[i].x, myCardsHolder[i].y, 0.0f);
                  switch (j)
                  {
                  case ACE_CARD:
                     Acard.draw(gl);
                     break;

                  case JACK_CARD:
                     Jcard.draw(gl);
                     break;

                  case QUEEN_CARD:
                     Qcard.draw(gl);
                     break;

                  case KING_CARD:
                     Kcard.draw(gl);
                     break;

                  default:
                     numberCards[j + 1].draw(gl);
                  }
                  gl.glPopMatrix();
               }
            }
            else
            {
               gl.glPushMatrix();
               gl.glTranslatef(myCardsHolder[i].x, myCardsHolder[i].y, 0.0f);
               cardFrame.drawDim(gl);
               gl.glPopMatrix();
            }
         }

         for (i = j = 0; i < 13; i++)
         {
            if (view.otherHand[i].visibility != CARD_VISIBILITY.HIDDEN)
            {
               j += view.otherHand[i].getCount();
            }
         }
         offset = (float)j * cardOffset;
         for (i = 0; i < j; i++)
         {
            gl.glPushMatrix();
            gl.glTranslatef((int)((float)otherHand.x + offset), otherHand.y, 0.0f);
            offset -= (cardOffset * 2.0f);
            cardBack.draw(gl);
            gl.glPopMatrix();
         }

         if (view.deckDeal < 52)
         {
            gl.glPushMatrix();
            gl.glTranslatef(drawCard.x, drawCard.y, 0.0f);
            cardBack.draw(gl);
            gl.glPopMatrix();
         }
         else
         {
            gl.glPushMatrix();
            gl.glTranslatef(drawCard.x, drawCard.y, 0.0f);
            cardFrame.drawDim(gl);
            gl.glPopMatrix();
         }

         gl.glPushMatrix();
         gl.glTranslatef(myScoreCard.x, myScoreCard.y, 0.0f);
         numberCards[view.myScore].draw(gl);
         gl.glPopMatrix();

         gl.glPushMatrix();
         gl.glTranslatef(otherScoreCard.x, otherScoreCard.y, 0.0f);
         numberCards[view.otherScore].draw(gl);
         gl.glPopMatrix();

         offset = (float)view.exchange.getCount() * cardOffset;
         if (offset > 0.0f)
         {
            for (k = 3; k >= 0; k--)
            {
               if (view.exchange.cards[k] != null)
               {
                  gl.glPushMatrix();
                  offset -= cardOffset;
                  gl.glTranslatef((int)((float)exchangeCard.x + offset), exchangeCard.y, 0.0f);
                  if (view.exchange.visibility == CARD_VISIBILITY.FACE_DOWN)
                  {
                     cardBack.draw(gl);
                  }
                  else
                  {
                     j = view.exchange.cards[k].rank;
                     cards[k][j].draw(gl);
                  }
                  gl.glPopMatrix();
               }
            }
         }
         else
         {
            gl.glPushMatrix();
            gl.glTranslatef(exchangeCard.x, exchangeCard.y, 0.0f);
            cardFrame.draw(gl);
            gl.glPopMatrix();
         }

         switch (view.myStage.visibility)
         {
         case FACE_UP:
         case FACE_DOWN:
            offset = (float)view.myStage.getCount() * cardOffset;
            if (offset > 0.0f)
            {
               for (k = 3; k >= 0; k--)
               {
                  if (view.myStage.cards[k] != null)
                  {
                     gl.glPushMatrix();
                     offset -= cardOffset;
                     gl.glTranslatef((int)((float)myStageCard.x + offset), myStageCard.y, 0.0f);
                     if (view.myStage.visibility == CARD_VISIBILITY.FACE_DOWN)
                     {
                        cardBack.draw(gl);
                     }
                     else
                     {
                        cards[k][view.myStage.rank].draw(gl);
                     }
                     gl.glPopMatrix();
                  }
               }
            }
            break;

         case RANK:
            gl.glPushMatrix();
            gl.glTranslatef(myStageCard.x, myStageCard.y, 0.0f);
            switch (view.myStage.rank)
            {
            case ACE_CARD:
               Acard.draw(gl);
               break;

            case JACK_CARD:
               Jcard.draw(gl);
               break;

            case QUEEN_CARD:
               Qcard.draw(gl);
               break;

            case KING_CARD:
               Kcard.draw(gl);
               break;

            default:
               numberCards[view.myStage.rank + 1].draw(gl);
            }
            gl.glPopMatrix();
            break;

         case HIDDEN:
            gl.glPushMatrix();
            gl.glTranslatef(myStageCard.x, myStageCard.y, 0.0f);
            cardFrame.draw(gl);
            gl.glPopMatrix();
         }

         switch (view.otherStage.visibility)
         {
         case FACE_UP:
         case FACE_DOWN:
            offset = (float)view.otherStage.getCount() * cardOffset;
            if (offset > 0.0f)
            {
               for (k = 3; k >= 0; k--)
               {
                  if (view.otherStage.cards[k] != null)
                  {
                     gl.glPushMatrix();
                     offset -= cardOffset;
                     gl.glTranslatef((int)((float)otherStageCard.x + offset), otherStageCard.y, 0.0f);
                     if (view.otherStage.visibility == CARD_VISIBILITY.FACE_DOWN)
                     {
                        cardBack.draw(gl);
                     }
                     else
                     {
                        cards[k][view.otherStage.rank].draw(gl);
                     }
                     gl.glPopMatrix();
                  }
               }
            }
            break;

         case RANK:
            gl.glPushMatrix();
            gl.glTranslatef(otherStageCard.x, otherStageCard.y, 0.0f);
            switch (view.otherStage.rank)
            {
            case ACE_CARD:
               Acard.draw(gl);
               break;

            case JACK_CARD:
               Jcard.draw(gl);
               break;

            case QUEEN_CARD:
               Qcard.draw(gl);
               break;

            case KING_CARD:
               Kcard.draw(gl);
               break;

            default:
               numberCards[view.otherStage.rank + 1].draw(gl);
            }
            gl.glPopMatrix();
            break;

         case HIDDEN:
            gl.glPushMatrix();
            gl.glTranslatef(otherStageCard.x, otherStageCard.y, 0.0f);
            cardFrame.draw(gl);
            gl.glPopMatrix();
         }

         // Draw reset button.
         gl.glPushMatrix();
         gl.glTranslatef(resetButtonDraw.x, resetButtonDraw.y, 0.0f);
         resetButton.draw(gl);
         gl.glPopMatrix();

         // Draw state-dependent arrows.
         switch (view.gameState)
         {
         case START:
            break;

         case MY_TURN:
            gl.glPushMatrix();
            gl.glTranslatef(myHandLeftArrow.x, myHandLeftArrow.y, 0.0f);
            gl.glRotatef(90.0f, 0.0f, 0.0f, 1.0f);
            arrow.draw(gl);
            gl.glPopMatrix();
            gl.glPushMatrix();
            gl.glTranslatef(myHandRightArrow.x, myHandRightArrow.y, 0.0f);
            gl.glRotatef(-90.0f, 0.0f, 0.0f, 1.0f);
            arrow.draw(gl);
            gl.glPopMatrix();
            gl.glPushMatrix();
            gl.glTranslatef(myHandToStageArrow.x, myHandToStageArrow.y, 0.0f);
            arrow.draw(gl);
            gl.glPopMatrix();
            break;

         case MY_HAND_TO_STAGE_SHIFT:
            gl.glPushMatrix();
            gl.glTranslatef(myHandLeftArrow.x, myHandLeftArrow.y, 0.0f);
            gl.glRotatef(90.0f, 0.0f, 0.0f, 1.0f);
            arrow.draw(gl);
            gl.glPopMatrix();
            gl.glPushMatrix();
            gl.glTranslatef(myHandRightArrow.x, myHandRightArrow.y, 0.0f);
            gl.glRotatef(-90.0f, 0.0f, 0.0f, 1.0f);
            arrow.draw(gl);
            gl.glPopMatrix();
            break;

         case MY_HAND_TO_STAGE_ONLY:
            gl.glPushMatrix();
            gl.glTranslatef(myHandToStageArrow.x, myHandToStageArrow.y, 0.0f);
            arrow.draw(gl);
            gl.glPopMatrix();
            break;

         case MY_STAGE_TO_HAND:
            gl.glPushMatrix();
            gl.glTranslatef(myHandToStageArrow.x, myHandToStageArrow.y, 0.0f);
            gl.glRotatef(180.0f, 0.0f, 0.0f, 1.0f);
            arrow.draw(gl);
            gl.glPopMatrix();
            break;

         case MY_STAGE_TO_EXCHANGE:
            gl.glPushMatrix();
            gl.glTranslatef(myStageToExchangeArrow.x, myStageToExchangeArrow.y, 0.0f);
            arrow.draw(gl);
            gl.glPopMatrix();
            break;

         case MY_STAGE_TO_SCORE:
            gl.glPushMatrix();
            gl.glTranslatef(myScoreToStageArrow.x, myScoreToStageArrow.y, 0.0f);
            gl.glRotatef(90.0f, 0.0f, 0.0f, 1.0f);
            arrow.draw(gl);
            gl.glPopMatrix();
            break;

         case EXCHANGE_TO_MY_STAGE:
            gl.glPushMatrix();
            gl.glTranslatef(myStageToExchangeArrow.x, myStageToExchangeArrow.y, 0.0f);
            gl.glRotatef(180.0f, 0.0f, 0.0f, 1.0f);
            arrow.draw(gl);
            gl.glPopMatrix();
            break;

         case OTHER_TURN:
            gl.glPushMatrix();
            gl.glTranslatef(otherHandToStageArrow.x, otherHandToStageArrow.y, 0.0f);
            gl.glRotatef(180.0f, 0.0f, 0.0f, 1.0f);
            arrow.draw(gl);
            gl.glPopMatrix();
            break;

         case OTHER_HAND_TO_STAGE:
            gl.glPushMatrix();
            gl.glTranslatef(otherHandToStageArrow.x, otherHandToStageArrow.y, 0.0f);
            gl.glRotatef(180.0f, 0.0f, 0.0f, 1.0f);
            arrow.draw(gl);
            gl.glPopMatrix();
            break;

         case OTHER_STAGE_TO_HAND:
            gl.glPushMatrix();
            gl.glTranslatef(otherHandToStageArrow.x, otherHandToStageArrow.y, 0.0f);
            arrow.draw(gl);
            gl.glPopMatrix();
            break;

         case OTHER_STAGE_TO_EXCHANGE:
            gl.glPushMatrix();
            gl.glTranslatef(otherStageToExchangeArrow.x, otherStageToExchangeArrow.y, 0.0f);
            gl.glRotatef(180.0f, 0.0f, 0.0f, 1.0f);
            arrow.draw(gl);
            gl.glPopMatrix();
            break;

         case OTHER_STAGE_TO_SCORE:
            gl.glPushMatrix();
            gl.glTranslatef(otherScoreToStageArrow.x, otherScoreToStageArrow.y, 0.0f);
            gl.glRotatef(90.0f, 0.0f, 0.0f, 1.0f);
            arrow.draw(gl);
            gl.glPopMatrix();
            break;

         case EXCHANGE_TO_OTHER_STAGE:
            gl.glPushMatrix();
            gl.glTranslatef(otherStageToExchangeArrow.x, otherStageToExchangeArrow.y, 0.0f);
            arrow.draw(gl);
            gl.glPopMatrix();
            break;

         case MY_DRAW_TO_EXCHANGE:
            gl.glPushMatrix();
            gl.glTranslatef(exchangeToDrawArrow.x, exchangeToDrawArrow.y, 0.0f);
            gl.glRotatef(90.0f, 0.0f, 0.0f, 1.0f);
            arrow.draw(gl);
            gl.glPopMatrix();
            break;

         case OTHER_DRAW_TO_EXCHANGE:
            gl.glPushMatrix();
            gl.glTranslatef(exchangeToDrawArrow.x, exchangeToDrawArrow.y, 0.0f);
            gl.glRotatef(90.0f, 0.0f, 0.0f, 1.0f);
            arrow.draw(gl);
            gl.glPopMatrix();
            break;

         case MY_WIN:
            drawText(gl, "You win!", myScoreCard.x, exchangeCard.y);
            break;

         case OTHER_WIN:
            drawText(gl, "You lose", myScoreCard.x, exchangeCard.y);
            break;

         case TIE:
            drawText(gl, "Tie", myScoreCard.x, exchangeCard.y);
            break;
         }
      }
   }

   // Card.
   public class Card
   {
      public int textureID;

      protected FloatBuffer vertexBuffer;
      protected float       vertices[] =
      {
         0.0f, 0.0f, 0.0f,
         0.0f, 1.0f, 0.0f,
         1.0f, 0.0f, 0.0f,
         1.0f, 1.0f, 0.0f
      };

      protected FloatBuffer textureBuffer;
      protected float       texture[] =
      {
         0.0f, 1.0f,
         0.0f, 0.0f,
         1.0f, 1.0f,
         1.0f, 0.0f
      };

      public Card(String imageFile, float width, float height,
                  GL10 gl, Context context)
      {
         createTexture(imageFile, gl, context);
         ByteBuffer byteBuffer = ByteBuffer.allocateDirect(vertices.length * 4);
         byteBuffer.order(ByteOrder.nativeOrder());
         vertexBuffer = byteBuffer.asFloatBuffer();
         for (int i = 0; i < VERTS; i++)
         {
            vertices[i * 3]     *= width;
            vertices[i * 3 + 1] *= height;
         }
         vertexBuffer.put(vertices);
         vertexBuffer.position(0);
         byteBuffer = ByteBuffer.allocateDirect(texture.length * 4);
         byteBuffer.order(ByteOrder.nativeOrder());
         textureBuffer = byteBuffer.asFloatBuffer();
         textureBuffer.put(texture);
         textureBuffer.position(0);
      }


      public Card() {}

      // Create texture from image file.
      private void createTexture(String imageFile, GL10 gl, Context context)
      {
         InputStream is = null;

         try
         {
            is = context.getAssets().open(imageFile);
         }
         catch (IOException e) {}
         if (is == null)
         {
            Log.d("GoFish", "Cannot open card texture file " + imageFile);
            return;
         }
         Bitmap b = null;
         try {
            b = BitmapFactory.decodeStream(is);
         }
         finally {
            try {
               is.close();
            }
            catch (IOException e) {}
         }
         if (b == null)
         {
            Log.d("GoFish", "Cannot create card texture bitmap from image file " + imageFile);
            return;
         }
         int w = b.getWidth();
         int h = b.getHeight();
         int w2, h2;
         for (w2 = 2; w2 < w; w2 *= 2) {}
         for (h2 = 2; h2 < h; h2 *= 2) {}
         Bitmap b2 = resize(b, w2, h2);
         b.recycle();
         b = b2;
         int[] textures = new int[1];
         gl.glGenTextures(1, textures, 0);
         textureID = textures[0];
         gl.glBindTexture(GL10.GL_TEXTURE_2D, textureID);
         gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_NEAREST);
         gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_LINEAR);
         GLUtils.texImage2D(GL10.GL_TEXTURE_2D, 0, b, 0);
         b.recycle();
      }


      // Resize bitmap.
      protected Bitmap resize(Bitmap bitmap, int newHeight, int newWidth)
      {
         int   width       = bitmap.getWidth();
         int   height      = bitmap.getHeight();
         float scaleWidth  = ((float)newWidth) / width;
         float scaleHeight = ((float)newHeight) / height;

         // create a matrix for the manipulation
         Matrix matrix = new Matrix();

         matrix.postScale(scaleWidth, scaleHeight);

         // resize the Bitmap
         Bitmap resizedBitmap = Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, false);
         return(resizedBitmap);
      }


      public void draw(GL10 gl)
      {
         gl.glBindTexture(GL10.GL_TEXTURE_2D, textureID);
         gl.glEnable(GL10.GL_TEXTURE_2D);
         gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
         gl.glEnableClientState(GL10.GL_TEXTURE_COORD_ARRAY);
         gl.glFrontFace(GL10.GL_CW);
         gl.glCullFace(GL10.GL_BACK);
         gl.glVertexPointer(3, GL10.GL_FLOAT, 0, vertexBuffer);
         gl.glTexCoordPointer(2, GL10.GL_FLOAT, 0, textureBuffer);
         gl.glDrawArrays(GL10.GL_TRIANGLE_STRIP, 0, vertices.length / 3);
         gl.glDisableClientState(GL10.GL_VERTEX_ARRAY);
         gl.glDisableClientState(GL10.GL_TEXTURE_COORD_ARRAY);
      }


      private final static int VERTS = 4;
   }

   // Card frame.
   class CardFrame
   {
      public CardFrame(float width, float height)
      {
         ByteBuffer vbb = ByteBuffer.allocateDirect(VERTS * 2 * 4);

         vbb.order(ByteOrder.nativeOrder());
         mFVertexBuffer = vbb.asFloatBuffer();

         float[] vertCoords =
         {
            0.0f, 0.0f,
            0.0f, 1.0f,
            1.0f, 1.0f,
            1.0f, 1.0f,
            1.0f, 0.0f,
            0.0f, 0.0f
         };

         for (int i = 0; i < VERTS; i++)
         {
            mFVertexBuffer.put(vertCoords[i * 2] * width);
            mFVertexBuffer.put(vertCoords[i * 2 + 1] * height);
         }
         mFVertexBuffer.position(0);
      }


      public void draw(GL10 gl)
      {
         gl.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
         drawCommon(gl);
      }


      public void drawDim(GL10 gl)
      {
         gl.glColor4f(0.5f, 0.5f, 0.5f, 1.0f);
         drawCommon(gl);
      }


      private void drawCommon(GL10 gl)
      {
         gl.glDisable(GL10.GL_TEXTURE_2D);
         gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
         gl.glFrontFace(GL10.GL_CW);
         gl.glCullFace(GL10.GL_BACK);
         gl.glVertexPointer(2, GL10.GL_FLOAT, 0, mFVertexBuffer);
         gl.glDrawArrays(GL10.GL_TRIANGLES, 0, VERTS);
         gl.glDisableClientState(GL10.GL_VERTEX_ARRAY);
      }


      private final static int VERTS = 6;
      private FloatBuffer      mFVertexBuffer;
   }

   // Arrow.
   class Arrow
   {
      public Arrow(float size)
      {
         ByteBuffer vbb = ByteBuffer.allocateDirect(VERTS * 2 * 4);

         vbb.order(ByteOrder.nativeOrder());
         mFVertexBuffer = vbb.asFloatBuffer();

         float[] vertCoords =
         {
            -0.5f, -0.433f,
            0.0f,   0.433f,
            0.5f, -0.433f
         };

         for (int i = 0; i < VERTS; i++)
         {
            for (int j = 0; j < 2; j++)
            {
               mFVertexBuffer.put(vertCoords[i * 2 + j] * size);
            }
         }
         mFVertexBuffer.position(0);
      }


      public void draw(GL10 gl)
      {
         gl.glDisable(GL10.GL_TEXTURE_2D);
         gl.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
         gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
         gl.glFrontFace(GL10.GL_CW);
         gl.glCullFace(GL10.GL_BACK);
         gl.glVertexPointer(2, GL10.GL_FLOAT, 0, mFVertexBuffer);
         gl.glDrawArrays(GL10.GL_TRIANGLES, 0, VERTS);
         gl.glDisableClientState(GL10.GL_VERTEX_ARRAY);
      }


      private final static int VERTS = 3;
      private FloatBuffer      mFVertexBuffer;
   }

   // Reset button.
   public class ResetButton
   {
      public int textureID;

      protected FloatBuffer vertexBuffer;
      protected float       vertices[] =
      {
         0.0f, 0.0f, 0.0f,
         0.0f, 1.0f, 0.0f,
         1.0f, 0.0f, 0.0f,
         1.0f, 1.0f, 0.0f
      };

      protected FloatBuffer textureBuffer;
      protected float       texture[] =
      {
         0.0f, 1.0f,
         0.0f, 0.0f,
         1.0f, 1.0f,
         1.0f, 0.0f
      };

      public ResetButton(String imageFile, float width, float height,
                         GL10 gl, Context context)
      {
         createTexture(imageFile, gl, context);
         ByteBuffer byteBuffer = ByteBuffer.allocateDirect(vertices.length * 4);
         byteBuffer.order(ByteOrder.nativeOrder());
         vertexBuffer = byteBuffer.asFloatBuffer();
         for (int i = 0; i < VERTS; i++)
         {
            vertices[i * 3]     *= width;
            vertices[i * 3 + 1] *= height;
         }
         vertexBuffer.put(vertices);
         vertexBuffer.position(0);
         byteBuffer = ByteBuffer.allocateDirect(texture.length * 4);
         byteBuffer.order(ByteOrder.nativeOrder());
         textureBuffer = byteBuffer.asFloatBuffer();
         textureBuffer.put(texture);
         textureBuffer.position(0);
      }


      public ResetButton() {}

      // Create texture from image file.
      private void createTexture(String imageFile, GL10 gl, Context context)
      {
         InputStream is = null;

         try
         {
            is = context.getAssets().open(imageFile);
         }
         catch (IOException e) {}
         if (is == null)
         {
            Log.d("GoFish", "Cannot open reset button texture file " + imageFile);
            return;
         }
         Bitmap b = null;
         try {
            b = BitmapFactory.decodeStream(is);
         }
         finally {
            try {
               is.close();
            }
            catch (IOException e) {}
         }
         if (b == null)
         {
            Log.d("GoFish", "Cannot create reset button texture bitmap from image file " + imageFile);
            return;
         }
         int[] textures = new int[1];
         gl.glGenTextures(1, textures, 0);
         textureID = textures[0];
         gl.glBindTexture(GL10.GL_TEXTURE_2D, textureID);
         gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_NEAREST);
         gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_LINEAR);
         GLUtils.texImage2D(GL10.GL_TEXTURE_2D, 0, b, 0);
         b.recycle();
      }


      public void draw(GL10 gl)
      {
         gl.glBindTexture(GL10.GL_TEXTURE_2D, textureID);
         gl.glEnable(GL10.GL_TEXTURE_2D);
         gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
         gl.glEnableClientState(GL10.GL_TEXTURE_COORD_ARRAY);
         gl.glFrontFace(GL10.GL_CW);
         gl.glCullFace(GL10.GL_BACK);
         gl.glVertexPointer(3, GL10.GL_FLOAT, 0, vertexBuffer);
         gl.glTexCoordPointer(2, GL10.GL_FLOAT, 0, textureBuffer);
         gl.glDrawArrays(GL10.GL_TRIANGLE_STRIP, 0, vertices.length / 3);
         gl.glDisableClientState(GL10.GL_VERTEX_ARRAY);
         gl.glDisableClientState(GL10.GL_TEXTURE_COORD_ARRAY);
      }


      private final static int VERTS = 4;
   }
}
