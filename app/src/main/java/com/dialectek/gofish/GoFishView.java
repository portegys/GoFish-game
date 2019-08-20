// Go Fish game view.

package com.dialectek.gofish;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Random;
import java.util.UUID;
import java.util.Vector;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.Point;
import android.opengl.GLSurfaceView;
import android.util.Log;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.widget.Toast;

public class GoFishView extends GLSurfaceView
implements GestureDetector.OnGestureListener
{
   private static final String LOG_TAG =
      GoFishView.class .getSimpleName();

   // Card suits.
   public final int CLUBS    = 0;
   public final int DIAMONDS = 1;
   public final int HEARTS   = 2;
   public final int SPADES   = 3;

   // Cards.
   public                 RankSet[] myHand;
   public                 RankSet[] otherHand;
   public Vector<RankSet> myScoreRanks;
   public Vector<RankSet> otherScoreRanks;
   public RankSet         myStage;
   public RankSet         otherStage;
   public RankSet         exchange;
   public                 Card[] deck;
   public int             deckDeal;
   public int             askRank;
   public int             myHandShift;

   // Game state.
   public enum GAME_STATE
   {
      START,
      MY_TURN,
      MY_HAND_TO_STAGE_SHIFT,
      MY_HAND_TO_STAGE_ONLY,
      MY_STAGE_TO_HAND,
      MY_STAGE_TO_EXCHANGE,
      MY_STAGE_TO_SCORE,
      EXCHANGE_TO_MY_STAGE,
      OTHER_TURN,
      OTHER_HAND_TO_STAGE,
      OTHER_STAGE_TO_HAND,
      OTHER_STAGE_TO_EXCHANGE,
      OTHER_STAGE_TO_SCORE,
      EXCHANGE_TO_OTHER_STAGE,
      MY_DRAW_TO_EXCHANGE,
      OTHER_DRAW_TO_EXCHANGE,
      MY_WIN,
      OTHER_WIN,
      TIE
   };
   public GAME_STATE gameState;
   public int        myScore, otherScore;

   // Player card rank knowledge.
   public enum RANK_KNOWLEDGE
   {
      DOES_HAVE,
      DOES_NOT_HAVE,
      MIGHT_HAVE,
      CANNOT_HAVE
   };
   public RANK_KNOWLEDGE[] rankKnowledge;

   // Context.
   Context context;

   // Renderer.
   GoFishRenderer renderer;

   // Random numbers.
   Random random;

   // Viewing manual?
   boolean viewManual;

   // Touch detection.
   static float    TOUCH_DISTANCE_SCALE = 0.05f;
   static float    SWIPE_DISTANCE_SCALE = 0.075f;
   GestureDetector gestures;
   float           moveX, moveY;
   boolean         swipeDone;

   // Save file path.
   String savePath;

   // Constructor.
   public GoFishView(Context context, String savePath, UUID id)
   {
      super(context);
      this.context  = context;
      this.savePath = savePath;

      // Random numbers.
      random = new Random();

      // Initialize game.
      init();

      // Not viewing manual.
      viewManual = false;

      // Create renderer.
      renderer = new GoFishRenderer(context, this);
      setRenderer(renderer);
      setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
      requestRender();

      // Create gesture detector.
      moveX     = moveY = 0.0f;
      swipeDone = false;
      gestures  = new GestureDetector(context, this);
   }


   // Initialize game.
   private void init()
   {
      // Create cards.
      int i, j, k;

      myHand = new RankSet[13];
      for (i = 0; i < 13; i++)
      {
         myHand[i] = new RankSet(i);
      }
      otherHand = new RankSet[13];
      for (i = 0; i < 13; i++)
      {
         otherHand[i] = new RankSet(i);
      }
      myScoreRanks          = new Vector<RankSet>();
      otherScoreRanks       = new Vector<RankSet>();
      myStage               = new RankSet();
      myStage.visibility    = CARD_VISIBILITY.HIDDEN;
      otherStage            = new RankSet();
      otherStage.visibility = CARD_VISIBILITY.HIDDEN;
      exchange              = new RankSet();
      exchange.visibility   = CARD_VISIBILITY.HIDDEN;
      deck     = new Card[52];
      deckDeal = k = 0;
      for (i = 0; i < 4; i++)
      {
         for (j = 0; j < 13; j++)
         {
            deck[k] = new Card(i, j);
            k++;
         }
      }

      // Shuffle deck.
      Card card;
      for (i = 0; i < 1000; i++)
      {
         j       = random.nextInt(52);
         k       = random.nextInt(52);
         card    = deck[j];
         deck[j] = deck[k];
         deck[k] = card;
      }

      // Deal cards.
      for (i = 0; i < 7; i++)
      {
         card = deck[deckDeal];
         deckDeal++;
         myHand[card.rank].add(card);
         myHand[card.rank].visibility = CARD_VISIBILITY.FACE_UP;
         card = deck[deckDeal];
         deckDeal++;
         otherHand[card.rank].add(card);
         otherHand[card.rank].visibility = CARD_VISIBILITY.FACE_DOWN;
      }
      askRank     = -1;
      myHandShift = 0;
      myScore     = otherScore = 0;

      // Check for score on deal.
      for (i = 0; i < 13; i++)
      {
         if (myHand[i].getCount() == 4)
         {
            myScore = 1;
            myHand[i].visibility = CARD_VISIBILITY.RANK;
            myScoreRanks.add(new RankSet(myHand[i]));
            myHand[i].visibility    = CARD_VISIBILITY.HIDDEN;
            otherHand[i].visibility = CARD_VISIBILITY.HIDDEN;
            break;
         }
      }
      for (i = 0; i < 13; i++)
      {
         if (otherHand[i].getCount() == 4)
         {
            otherScore = 1;
            otherHand[i].visibility = CARD_VISIBILITY.RANK;
            otherScoreRanks.add(new RankSet(otherHand[i]));
            myHand[i].visibility    = CARD_VISIBILITY.HIDDEN;
            otherHand[i].visibility = CARD_VISIBILITY.HIDDEN;
            break;
         }
      }

      // Start game.
      if (myHand[6].visibility == CARD_VISIBILITY.FACE_UP)
      {
         gameState = GAME_STATE.MY_TURN;
      }
      else
      {
         gameState = GAME_STATE.MY_HAND_TO_STAGE_SHIFT;
      }

      rankKnowledge = new RANK_KNOWLEDGE[13];
      for (i = 0; i < 13; i++)
      {
         if (myHand[i].visibility == CARD_VISIBILITY.HIDDEN)
         {
            rankKnowledge[i] = RANK_KNOWLEDGE.CANNOT_HAVE;
         }
         else
         {
            rankKnowledge[i] = RANK_KNOWLEDGE.MIGHT_HAVE;
         }
      }
   }


   // Key press events.
   @Override
   public boolean onKeyDown(int keyCode, KeyEvent event)
   {
      switch (keyCode)
      {
      case KeyEvent.KEYCODE_CALL:
      case KeyEvent.KEYCODE_CAMERA:
      case KeyEvent.KEYCODE_ENDCALL:
      case KeyEvent.KEYCODE_ENVELOPE:
      case KeyEvent.KEYCODE_EXPLORER:
      case KeyEvent.KEYCODE_FOCUS:
      case KeyEvent.KEYCODE_HOME:
      case KeyEvent.KEYCODE_HEADSETHOOK:
      case KeyEvent.KEYCODE_MENU:
      case KeyEvent.KEYCODE_POWER:
      case KeyEvent.KEYCODE_VOLUME_DOWN:
      case KeyEvent.KEYCODE_VOLUME_UP:
         return(super.onKeyDown(keyCode, event));

      // Reset?
      case KeyEvent.KEYCODE_BACK:
         synchronized (renderer)
         {
            init();
            requestRender();
         }
         return(true);

      // View manual?
      case KeyEvent.KEYCODE_SEARCH:
         return(viewManual());
      }

      // Move input.
      doMoveInput(keyCode);
      return(true);
   }


   // Move input.
   void doMoveInput(int keyCode)
   {
      // Game move.
      switch (gameState)
      {
      case START:
         break;

      case MY_TURN:
         if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT)
         {
            shiftHand(-1);
         }
         else if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT)
         {
            shiftHand(1);
         }
         else if (keyCode == KeyEvent.KEYCODE_DPAD_UP)
         {
            doMove();
         }
         break;

      case MY_HAND_TO_STAGE_SHIFT:
         if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT)
         {
            shiftHand(-1);
         }
         else if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT)
         {
            shiftHand(1);
         }
         break;

      case MY_HAND_TO_STAGE_ONLY:
         if (keyCode == KeyEvent.KEYCODE_DPAD_UP)
         {
            doMove();
         }
         break;

      case MY_STAGE_TO_HAND:
         if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN)
         {
            doMove();
         }
         break;

      case MY_STAGE_TO_EXCHANGE:
         if (keyCode == KeyEvent.KEYCODE_DPAD_UP)
         {
            doMove();
         }
         break;

      case MY_STAGE_TO_SCORE:
         if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT)
         {
            doMove();
         }
         break;

      case EXCHANGE_TO_MY_STAGE:
         if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN)
         {
            doMove();
         }
         break;

      case OTHER_TURN:
         if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN)
         {
            doMove();
         }
         break;

      case OTHER_HAND_TO_STAGE:
         if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN)
         {
            doMove();
         }
         break;

      case OTHER_STAGE_TO_HAND:
         if (keyCode == KeyEvent.KEYCODE_DPAD_UP)
         {
            doMove();
         }
         break;

      case OTHER_STAGE_TO_EXCHANGE:
         if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN)
         {
            doMove();
         }
         break;

      case OTHER_STAGE_TO_SCORE:
         if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT)
         {
            doMove();
         }
         break;

      case EXCHANGE_TO_OTHER_STAGE:
         if (keyCode == KeyEvent.KEYCODE_DPAD_UP)
         {
            doMove();
         }
         break;

      case MY_DRAW_TO_EXCHANGE:
         if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT)
         {
            doMove();
         }
         break;

      case OTHER_DRAW_TO_EXCHANGE:
         if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT)
         {
            doMove();
         }
         break;

      case MY_WIN:
         break;

      case OTHER_WIN:
         break;

      case TIE:
         break;
      }
   }


   // Touch event.
   @Override
   public boolean onTouchEvent(final MotionEvent event)
   {
      // Process gestures.
      gestures.onTouchEvent(event);

      // Detect completion of gesture.
      if (event.getAction() == MotionEvent.ACTION_UP)
      {
         if (!swipeDone)
         {
            onTap(event);
         }
         moveX     = moveY = 0.0f;
         swipeDone = false;
      }
      return(true);
   }


   // Single tap.
   public boolean onTap(MotionEvent event)
   {
      Point touch = new Point();

      touch.x = (int)event.getX();
      touch.y = (int)event.getY();

      // Reset?
      if (touchPoint(touch, renderer.playSurface.resetButtonTouch))
      {
         synchronized (renderer)
         {
            init();
            requestRender();
         }
         return(true);
      }

      // Touch arrow?
      switch (gameState)
      {
      case START:
         break;

      case MY_TURN:
         if (touchPoint(touch, renderer.playSurface.myHandLeftArrow))
         {
            shiftHand(-1);
         }
         else if (touchPoint(touch, renderer.playSurface.myHandRightArrow))
         {
            shiftHand(1);
         }
         else if (touchPoint(touch, renderer.playSurface.myHandToStageArrow))
         {
            doMove();
         }
         break;

      case MY_HAND_TO_STAGE_SHIFT:
         if (touchPoint(touch, renderer.playSurface.myHandLeftArrow))
         {
            shiftHand(-1);
         }
         else if (touchPoint(touch, renderer.playSurface.myHandRightArrow))
         {
            shiftHand(1);
         }
         break;

      case MY_HAND_TO_STAGE_ONLY:
         if (touchPoint(touch, renderer.playSurface.myHandToStageArrow))
         {
            doMove();
         }
         break;

      case MY_STAGE_TO_HAND:
         if (touchPoint(touch, renderer.playSurface.myHandToStageArrow))
         {
            doMove();
         }
         break;

      case MY_STAGE_TO_EXCHANGE:
         if (touchPoint(touch, renderer.playSurface.myStageToExchangeArrow))
         {
            doMove();
         }
         break;

      case MY_STAGE_TO_SCORE:
         if (touchPoint(touch, renderer.playSurface.myScoreToStageArrow))
         {
            doMove();
         }
         break;

      case EXCHANGE_TO_MY_STAGE:
         if (touchPoint(touch, renderer.playSurface.myStageToExchangeArrow))
         {
            doMove();
         }
         break;

      case OTHER_TURN:
         if (touchPoint(touch, renderer.playSurface.otherHandToStageArrow))
         {
            doMove();
         }
         break;

      case OTHER_HAND_TO_STAGE:
         if (touchPoint(touch, renderer.playSurface.otherHandToStageArrow))
         {
            doMove();
         }
         break;

      case OTHER_STAGE_TO_HAND:
         if (touchPoint(touch, renderer.playSurface.otherHandToStageArrow))
         {
            doMove();
         }
         break;

      case OTHER_STAGE_TO_EXCHANGE:
         if (touchPoint(touch, renderer.playSurface.otherStageToExchangeArrow))
         {
            doMove();
         }
         break;

      case OTHER_STAGE_TO_SCORE:
         if (touchPoint(touch, renderer.playSurface.otherScoreToStageArrow))
         {
            doMove();
         }
         break;

      case EXCHANGE_TO_OTHER_STAGE:
         if (touchPoint(touch, renderer.playSurface.otherStageToExchangeArrow))
         {
            doMove();
         }
         break;

      case MY_DRAW_TO_EXCHANGE:
         if (touchPoint(touch, renderer.playSurface.exchangeToDrawArrow))
         {
            doMove();
         }
         break;

      case OTHER_DRAW_TO_EXCHANGE:
         if (touchPoint(touch, renderer.playSurface.exchangeToDrawArrow))
         {
            doMove();
         }
         break;

      case MY_WIN:
         break;

      case OTHER_WIN:
         break;

      case TIE:
         break;
      }
      return(true);
   }


   // Scroll.
   public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY)
   {
      int keyCode;

      moveX += distanceX;
      moveY += distanceY;

      if (swipeDone) { return(true); }

      float swipeDistance = 0.0f;
      if (renderer.windowWidth < renderer.windowHeight)
      {
         swipeDistance = renderer.windowWidth * SWIPE_DISTANCE_SCALE;
      }
      else
      {
         swipeDistance = renderer.windowHeight * SWIPE_DISTANCE_SCALE;
      }

      if (Math.abs(moveY) >= Math.abs(moveX))
      {
         if (moveY >= swipeDistance)
         {
            swipeDone = true;
            keyCode   = KeyEvent.KEYCODE_DPAD_UP;
            doMoveInput(keyCode);
         }
         else if (moveY <= -swipeDistance)
         {
            swipeDone = true;
            keyCode   = KeyEvent.KEYCODE_DPAD_DOWN;
            doMoveInput(keyCode);
         }
      }
      else
      {
         if (moveX >= swipeDistance)
         {
            swipeDone = true;
            keyCode   = KeyEvent.KEYCODE_DPAD_LEFT;
            doMoveInput(keyCode);
         }
         else if (moveX <= -swipeDistance)
         {
            swipeDone = true;
            keyCode   = KeyEvent.KEYCODE_DPAD_RIGHT;
            doMoveInput(keyCode);
         }
      }
      return(true);
   }


   // Fling.
   public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY)
   {
      return(true);
   }


   public void onLongPress(MotionEvent event)
   {
   }


   public void onShowPress(MotionEvent event)
   {
   }


   public boolean onSingleTapUp(MotionEvent event)
   {
      return(true);
   }


   public boolean onDown(MotionEvent event)
   {
      return(true);
   }


   // Point touched?
   private boolean touchPoint(Point touchPoint, Point targetPoint)
   {
      float touchDistance = 0.0f;

      if (renderer.windowWidth < renderer.windowHeight)
      {
         touchDistance = renderer.windowWidth * TOUCH_DISTANCE_SCALE;
      }
      else
      {
         touchDistance = renderer.windowHeight * TOUCH_DISTANCE_SCALE;
      }
      if ((Math.abs((float)touchPoint.x - (float)targetPoint.x) <= touchDistance) &&
          (Math.abs((float)touchPoint.y - (float)renderer.windowHeight + (float)targetPoint.y) <= touchDistance))
      {
         return(true);
      }
      else
      {
         return(false);
      }
   }


   // Shift hand.
   private void shiftHand(int direction)
   {
      synchronized (renderer)
      {
         myHandShift += direction;
         int rank = getShiftIndex(6);
         if (myHand[rank].visibility == CARD_VISIBILITY.FACE_UP)
         {
            if (otherStage.visibility == CARD_VISIBILITY.RANK)
            {
               if (myHand[rank].rank == otherStage.rank)
               {
                  gameState = GAME_STATE.MY_HAND_TO_STAGE_ONLY;
               }
               else
               {
                  gameState = GAME_STATE.MY_HAND_TO_STAGE_SHIFT;
               }
            }
            else
            {
               if (myStage.getCount() == 0)
               {
                  gameState = GAME_STATE.MY_TURN;
               }
               else
               {
                  if (myStage.rank == rank)
                  {
                     gameState = GAME_STATE.MY_HAND_TO_STAGE_ONLY;
                  }
                  else
                  {
                     gameState = GAME_STATE.MY_HAND_TO_STAGE_SHIFT;
                  }
               }
            }
         }
         else
         {
            gameState = GAME_STATE.MY_HAND_TO_STAGE_SHIFT;
         }
         requestRender();
      }
   }


   // Do game move.
   private void doMove()
   {
      int i, j, rank;

      synchronized (renderer)
      {
         switch (gameState)
         {
         case START:
            break;

         case MY_TURN:
            rank                = getShiftIndex(6);
            askRank             = rank;
            rankKnowledge[rank] = RANK_KNOWLEDGE.DOES_HAVE;
            myStage.rank        = rank;
            myStage.visibility  = CARD_VISIBILITY.RANK;
            if (otherHand[rank].getCount() > 0)
            {
               gameState = GAME_STATE.OTHER_HAND_TO_STAGE;
            }
            else
            {
               gameState = GAME_STATE.MY_DRAW_TO_EXCHANGE;
            }
            break;

         case MY_HAND_TO_STAGE_ONLY:
            rank = getShiftIndex(6);
            myStage.transfer(myHand[rank]);
            myStage.visibility      = CARD_VISIBILITY.FACE_UP;
            myHand[rank].visibility = CARD_VISIBILITY.RANK;
            if (otherStage.visibility == CARD_VISIBILITY.RANK)
            {
               gameState = GAME_STATE.MY_STAGE_TO_EXCHANGE;
            }
            else
            {
               gameState = GAME_STATE.MY_STAGE_TO_SCORE;
            }
            break;

         case MY_STAGE_TO_HAND:
            rank = myStage.rank;
            myHand[rank].transfer(myStage);
            myHand[rank].visibility = CARD_VISIBILITY.FACE_UP;
            if (myStage.visibility == CARD_VISIBILITY.FACE_UP)
            {
               gameState = GAME_STATE.MY_TURN;
            }
            else
            {
               gameState = GAME_STATE.OTHER_TURN;
            }
            myStage.visibility = CARD_VISIBILITY.HIDDEN;
            for (i = 0; i < 13; i++)
            {
               if (otherHand[i].visibility != CARD_VISIBILITY.HIDDEN)
               {
                  if (otherHand[i].getCount() > 0) { break; }
               }
            }
            if ((deckDeal == 52) || (i == 13))
            {
               if (myScore > otherScore)
               {
                  gameState = GAME_STATE.MY_WIN;
               }
               else if (myScore == otherScore)
               {
                  gameState = GAME_STATE.TIE;
               }
               else
               {
                  gameState = GAME_STATE.OTHER_WIN;
               }
            }
            break;

         case MY_STAGE_TO_EXCHANGE:
            exchange.transfer(myStage);
            exchange.visibility = myStage.visibility;
            myStage.visibility  = CARD_VISIBILITY.HIDDEN;
            gameState           = GAME_STATE.EXCHANGE_TO_OTHER_STAGE;
            break;

         case MY_STAGE_TO_SCORE:
            myScore++;
            rank = myStage.rank;
            rankKnowledge[rank] = RANK_KNOWLEDGE.CANNOT_HAVE;
            myStage.visibility  = CARD_VISIBILITY.RANK;
            myScoreRanks.add(new RankSet(myStage));
            myStage.visibility         = CARD_VISIBILITY.HIDDEN;
            myHand[rank].visibility    = CARD_VISIBILITY.HIDDEN;
            otherHand[rank].visibility = CARD_VISIBILITY.HIDDEN;
            if (askRank == rank)
            {
               if (myHand[getShiftIndex(6)].getCount() > 0)
               {
                  gameState = GAME_STATE.MY_TURN;
               }
               else
               {
                  gameState = GAME_STATE.MY_HAND_TO_STAGE_SHIFT;
               }
            }
            else
            {
               gameState = GAME_STATE.OTHER_TURN;
            }
            for (i = 0; i < 13; i++)
            {
               if (myHand[i].visibility != CARD_VISIBILITY.HIDDEN)
               {
                  if (myHand[i].getCount() > 0) { break; }
               }
            }
            for (j = 0; j < 13; j++)
            {
               if (otherHand[j].visibility != CARD_VISIBILITY.HIDDEN)
               {
                  if (otherHand[j].getCount() > 0) { break; }
               }
            }
            if ((deckDeal == 52) || (i == 13) || (j == 13))
            {
               if (myScore > otherScore)
               {
                  gameState = GAME_STATE.MY_WIN;
               }
               else if (myScore == otherScore)
               {
                  gameState = GAME_STATE.TIE;
               }
               else
               {
                  gameState = GAME_STATE.OTHER_WIN;
               }
            }
            break;

         case EXCHANGE_TO_MY_STAGE:
            myStage.transfer(exchange);
            myStage.visibility  = exchange.visibility;
            exchange.visibility = CARD_VISIBILITY.HIDDEN;
            if (myStage.visibility == CARD_VISIBILITY.FACE_DOWN)
            {
               if ((myStage.getCount() + myHand[myStage.rank].getCount()) == 4)
               {
                  myStage.visibility = CARD_VISIBILITY.FACE_UP;
                  gameState          = GAME_STATE.MY_HAND_TO_STAGE_SHIFT;
               }
               else
               {
                  for (i = 0; i < 13; i++)
                  {
                     if (rankKnowledge[i] == RANK_KNOWLEDGE.DOES_NOT_HAVE)
                     {
                        rankKnowledge[i] = RANK_KNOWLEDGE.MIGHT_HAVE;
                     }
                  }
                  gameState = GAME_STATE.MY_STAGE_TO_HAND;
               }
            }
            else
            {
               if ((myStage.getCount() + myHand[myStage.rank].getCount()) == 4)
               {
                  gameState = GAME_STATE.MY_HAND_TO_STAGE_ONLY;
               }
               else
               {
                  gameState = GAME_STATE.MY_STAGE_TO_HAND;
               }
            }
            break;

         case OTHER_TURN:
            rank                  = getAskRank();
            askRank               = rank;
            rankKnowledge[rank]   = RANK_KNOWLEDGE.DOES_NOT_HAVE;
            otherStage.rank       = rank;
            otherStage.visibility = CARD_VISIBILITY.RANK;
            if (myHand[rank].getCount() > 0)
            {
               if (getShiftIndex(6) == rank)
               {
                  gameState = GAME_STATE.MY_HAND_TO_STAGE_ONLY;
               }
               else
               {
                  gameState = GAME_STATE.MY_HAND_TO_STAGE_SHIFT;
               }
            }
            else
            {
               gameState = GAME_STATE.OTHER_DRAW_TO_EXCHANGE;
            }
            break;

         case OTHER_HAND_TO_STAGE:
            if (otherStage.getCount() > 0)
            {
               rank = otherStage.rank;
               otherStage.transfer(otherHand[rank]);
               otherStage.visibility      = CARD_VISIBILITY.FACE_UP;
               otherHand[rank].visibility = CARD_VISIBILITY.RANK;
               gameState = GAME_STATE.OTHER_STAGE_TO_SCORE;
            }
            else
            {
               rank = myStage.rank;
               otherStage.transfer(otherHand[rank]);
               otherStage.visibility      = CARD_VISIBILITY.FACE_UP;
               otherHand[rank].visibility = CARD_VISIBILITY.RANK;
               gameState = GAME_STATE.OTHER_STAGE_TO_EXCHANGE;
            }
            break;

         case OTHER_STAGE_TO_HAND:
            rank = otherStage.rank;
            otherHand[rank].transfer(otherStage);
            otherHand[rank].visibility = CARD_VISIBILITY.FACE_DOWN;
            if (otherStage.visibility == CARD_VISIBILITY.FACE_UP)
            {
               gameState = GAME_STATE.OTHER_TURN;
            }
            else
            {
               if (myHand[getShiftIndex(6)].getCount() > 0)
               {
                  gameState = GAME_STATE.MY_TURN;
               }
               else
               {
                  gameState = GAME_STATE.MY_HAND_TO_STAGE_SHIFT;
               }
            }
            otherStage.visibility = CARD_VISIBILITY.HIDDEN;
            for (i = 0; i < 13; i++)
            {
               if (myHand[i].visibility != CARD_VISIBILITY.HIDDEN)
               {
                  if (myHand[i].getCount() > 0) { break; }
               }
            }
            if ((deckDeal == 52) || (i == 13))
            {
               if (myScore > otherScore)
               {
                  gameState = GAME_STATE.MY_WIN;
               }
               else if (myScore == otherScore)
               {
                  gameState = GAME_STATE.TIE;
               }
               else
               {
                  gameState = GAME_STATE.OTHER_WIN;
               }
            }
            break;

         case OTHER_STAGE_TO_EXCHANGE:
            exchange.transfer(otherStage);
            exchange.visibility   = otherStage.visibility;
            otherStage.visibility = CARD_VISIBILITY.HIDDEN;
            gameState             = GAME_STATE.EXCHANGE_TO_MY_STAGE;
            break;

         case OTHER_STAGE_TO_SCORE:
            otherScore++;
            rank = otherStage.rank;
            rankKnowledge[rank]   = RANK_KNOWLEDGE.CANNOT_HAVE;
            otherStage.visibility = CARD_VISIBILITY.RANK;
            otherScoreRanks.add(new RankSet(otherStage));
            otherStage.visibility      = CARD_VISIBILITY.HIDDEN;
            otherHand[rank].visibility = CARD_VISIBILITY.HIDDEN;
            myHand[rank].visibility    = CARD_VISIBILITY.HIDDEN;
            if (askRank == rank)
            {
               gameState = GAME_STATE.OTHER_TURN;
            }
            else
            {
               if (myHand[getShiftIndex(6)].getCount() > 0)
               {
                  gameState = GAME_STATE.MY_TURN;
               }
               else
               {
                  gameState = GAME_STATE.MY_HAND_TO_STAGE_SHIFT;
               }
            }
            for (i = 0; i < 13; i++)
            {
               if (myHand[i].visibility != CARD_VISIBILITY.HIDDEN)
               {
                  if (myHand[i].getCount() > 0) { break; }
               }
            }
            for (j = 0; j < 13; j++)
            {
               if (otherHand[j].visibility != CARD_VISIBILITY.HIDDEN)
               {
                  if (otherHand[j].getCount() > 0) { break; }
               }
            }
            if ((deckDeal == 52) || (i == 13) || (j == 13))
            {
               if (myScore > otherScore)
               {
                  gameState = GAME_STATE.MY_WIN;
               }
               else if (myScore == otherScore)
               {
                  gameState = GAME_STATE.TIE;
               }
               else
               {
                  gameState = GAME_STATE.OTHER_WIN;
               }
            }
            break;

         case EXCHANGE_TO_OTHER_STAGE:
            otherStage.transfer(exchange);
            otherStage.visibility = exchange.visibility;
            exchange.visibility   = CARD_VISIBILITY.HIDDEN;
            if (otherStage.visibility == CARD_VISIBILITY.FACE_DOWN)
            {
               if ((otherStage.getCount() + otherHand[otherStage.rank].getCount()) == 4)
               {
                  otherStage.visibility = CARD_VISIBILITY.FACE_UP;
                  gameState             = GAME_STATE.OTHER_HAND_TO_STAGE;
               }
               else
               {
                  gameState = GAME_STATE.OTHER_STAGE_TO_HAND;
               }
            }
            else
            {
               if ((otherStage.getCount() + otherHand[otherStage.rank].getCount()) == 4)
               {
                  gameState = GAME_STATE.OTHER_HAND_TO_STAGE;
               }
               else
               {
                  gameState = GAME_STATE.OTHER_STAGE_TO_HAND;
               }
            }
            break;

         case MY_DRAW_TO_EXCHANGE:
            exchange.add(deck[deckDeal]);
            deckDeal++;
            if (myStage.rank == exchange.rank)
            {
               exchange.visibility = CARD_VISIBILITY.FACE_UP;
            }
            else
            {
               exchange.visibility = CARD_VISIBILITY.FACE_DOWN;
            }
            gameState = GAME_STATE.EXCHANGE_TO_MY_STAGE;
            break;

         case OTHER_DRAW_TO_EXCHANGE:
            exchange.add(deck[deckDeal]);
            deckDeal++;
            if (otherStage.rank == exchange.rank)
            {
               exchange.visibility = CARD_VISIBILITY.FACE_UP;
            }
            else
            {
               exchange.visibility = CARD_VISIBILITY.FACE_DOWN;
            }
            gameState = GAME_STATE.EXCHANGE_TO_OTHER_STAGE;
            break;

         case MY_WIN:
            break;

         case OTHER_WIN:
            break;

         case TIE:
            break;
         }
         requestRender();
      }
   }


   // Card.
   public class Card
   {
      public int suit;
      public int rank;

      public Card(int suit, int rank)
      {
         this.suit = suit;
         this.rank = rank;
      }


      public Card()
      {
         suit = rank = 0;
      }


      public void load(BufferedReader in) throws IOException
      {
         suit = Integer.parseInt(in.readLine());
         rank = Integer.parseInt(in.readLine());
      }


      public void save(PrintWriter out)
      {
         out.println(suit + "");
         out.println(rank + "");
      }
   };

   // Card visibility.
   public enum CARD_VISIBILITY
   {
      FACE_UP,
      FACE_DOWN,
      RANK,
      HIDDEN
   };

   // Card rank set.
   public class RankSet
   {
      public int      rank;
      public          Card[] cards;
      CARD_VISIBILITY visibility;

      public RankSet(int rank)
      {
         this.rank = rank;
         cards     = new Card[4];
         clear();
         visibility = CARD_VISIBILITY.RANK;
      }


      public RankSet()
      {
         rank  = 0;
         cards = new Card[4];
         clear();
         visibility = CARD_VISIBILITY.RANK;
      }


      public RankSet(RankSet from)
      {
         rank  = from.rank;
         cards = new Card[4];
         clear();
         for (int i = 0; i < 4; i++)
         {
            cards[i] = from.cards[i];
            if (from.cards[i] != null)
            {
               from.cards[i] = null;
            }
         }
         visibility = from.visibility;
      }


      public void add(Card card)
      {
         cards[card.suit] = card;
         rank             = card.rank;
      }


      public int getCount()
      {
         int count = 0;

         for (int i = 0; i < 4; i++)
         {
            if (cards[i] != null)
            {
               count++;
            }
         }
         return(count);
      }


      public void transfer(RankSet from)
      {
         for (int i = 0; i < 4; i++)
         {
            if (from.cards[i] != null)
            {
               cards[i]      = from.cards[i];
               from.cards[i] = null;
               rank          = from.rank;
            }
         }
      }


      public void clear()
      {
         for (int i = 0; i < 4; i++)
         {
            cards[i] = null;
         }
      }


      public void load(BufferedReader in) throws IOException
      {
         int i;

         rank = Integer.parseInt(in.readLine());
         clear();
         for (i = 0; i < 4; i++)
         {
            if (in.readLine().equals("1"))
            {
               cards[i] = new Card();
               cards[i].load(in);
            }
         }
         i = Integer.parseInt(in.readLine());
         switch (i)
         {
         case 0:
            visibility = CARD_VISIBILITY.FACE_UP;
            break;

         case 1:
            visibility = CARD_VISIBILITY.FACE_DOWN;
            break;

         case 2:
            visibility = CARD_VISIBILITY.RANK;
            break;

         case 3:
            visibility = CARD_VISIBILITY.HIDDEN;
            break;
         }
      }


      public void save(PrintWriter out)
      {
         out.println(rank + "");
         for (int i = 0; i < 4; i++)
         {
            if (cards[i] != null)
            {
               out.println("1");
               cards[i].save(out);
            }
            else
            {
               out.println("0");
            }
         }
         switch (visibility)
         {
         case FACE_UP:
            out.println("0");
            break;

         case FACE_DOWN:
            out.println("1");
            break;

         case RANK:
            out.println("2");
            break;

         case HIDDEN:
            out.println("3");
            break;
         }
      }
   };

   // Get ask card rank.
   private int getAskRank()
   {
      int i, rank;

      for (i = 0, rank = random.nextInt(13); i < 13; i++, rank = (rank + 1) % 13)
      {
         if ((otherHand[rank].getCount() > 0) &&
             (rankKnowledge[rank] == RANK_KNOWLEDGE.DOES_HAVE))
         {
            return(rank);
         }
      }
      for (i = 0, rank = random.nextInt(13); i < 13; i++, rank = (rank + 1) % 13)
      {
         if ((otherHand[rank].getCount() > 0) &&
             (rankKnowledge[rank] == RANK_KNOWLEDGE.MIGHT_HAVE))
         {
            return(rank);
         }
      }
      for (i = 0, rank = random.nextInt(13); i < 13; i++, rank = (rank + 1) % 13)
      {
         if (otherHand[rank].getCount() > 0)
         {
            return(rank);
         }
      }
      return(0);
   }


   // Get shifted card index.
   private int getShiftIndex(int i)
   {
      int j = i - myHandShift;

      while (j < 0) { j += 13; }
      j %= 13;
      return(j);
   }


   @Override
   public void onResume()
   {
      super.onResume();

      // Load game.
      loadGame();

      // Set focus.
      setFocusable(true);
      setFocusableInTouchMode(true);
      requestFocus();

      // Activate renderer.
      renderer.drawablesValid = false;
      requestRender();
   }


   @Override
   public void onPause()
   {
      super.onPause();

      // Save for later resumption.
      saveGame();
   }


   // View game manual.
   boolean viewManual()
   {
      String manualPath = "doc/gofish.txt";
      Intent intent     = new Intent(Intent.ACTION_VIEW);

      intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
      intent.putExtra("manual_path", manualPath);
      intent.setClassName("com.dialectek.gofish", "com.dialectek.gofish.ManualViewer");
      viewManual = true;
      try {
         context.startActivity(intent);
      }
      catch (ActivityNotFoundException e) {
         viewManual = false;
         Toast.makeText(context,
                        "No activity available to view " + manualPath,
                        Toast.LENGTH_SHORT).show();
      }
      return(true);
   }


   // Load game.
   void loadGame()
   {
      int     i, n;
      RankSet rankSet;

      try
      {
         BufferedReader in = new BufferedReader(new FileReader(savePath));
         for (i = 0; i < 13; i++)
         {
            myHand[i].load(in);
         }
         for (i = 0; i < 13; i++)
         {
            otherHand[i].load(in);
         }
         n = Integer.parseInt(in.readLine());
         myScoreRanks.clear();
         for (i = 0; i < n; i++)
         {
            rankSet = new RankSet();
            rankSet.load(in);
            myScoreRanks.add(rankSet);
         }
         n = Integer.parseInt(in.readLine());
         otherScoreRanks.clear();
         for (i = 0; i < n; i++)
         {
            rankSet = new RankSet();
            rankSet.load(in);
            otherScoreRanks.add(rankSet);
         }
         myStage.load(in);
         otherStage.load(in);
         exchange.load(in);
         for (i = 0; i < 52; i++)
         {
            deck[i].load(in);
         }
         deckDeal    = Integer.parseInt(in.readLine());
         askRank     = Integer.parseInt(in.readLine());
         myHandShift = Integer.parseInt(in.readLine());
         i           = Integer.parseInt(in.readLine());
         switch (i)
         {
         case 0:
            gameState = GAME_STATE.START;
            break;

         case 1:
            gameState = GAME_STATE.MY_TURN;
            break;

         case 2:
            gameState = GAME_STATE.MY_HAND_TO_STAGE_SHIFT;
            break;

         case 3:
            gameState = GAME_STATE.MY_HAND_TO_STAGE_ONLY;
            break;

         case 4:
            gameState = GAME_STATE.MY_STAGE_TO_HAND;
            break;

         case 5:
            gameState = GAME_STATE.MY_STAGE_TO_EXCHANGE;
            break;

         case 6:
            gameState = GAME_STATE.MY_STAGE_TO_SCORE;
            break;

         case 7:
            gameState = GAME_STATE.EXCHANGE_TO_MY_STAGE;
            break;

         case 8:
            gameState = GAME_STATE.OTHER_TURN;
            break;

         case 9:
            gameState = GAME_STATE.OTHER_HAND_TO_STAGE;
            break;

         case 10:
            gameState = GAME_STATE.OTHER_STAGE_TO_HAND;
            break;

         case 11:
            gameState = GAME_STATE.OTHER_STAGE_TO_EXCHANGE;
            break;

         case 12:
            gameState = GAME_STATE.OTHER_STAGE_TO_SCORE;
            break;

         case 13:
            gameState = GAME_STATE.EXCHANGE_TO_OTHER_STAGE;
            break;

         case 14:
            gameState = GAME_STATE.MY_DRAW_TO_EXCHANGE;
            break;

         case 15:
            gameState = GAME_STATE.OTHER_DRAW_TO_EXCHANGE;
            break;

         case 16:
            gameState = GAME_STATE.MY_WIN;
            break;

         case 17:
            gameState = GAME_STATE.OTHER_WIN;
            break;

         case 18:
            gameState = GAME_STATE.TIE;
            break;
         }
         myScore    = Integer.parseInt(in.readLine());
         otherScore = Integer.parseInt(in.readLine());
         for (i = 0; i < 13; i++)
         {
            switch (Integer.parseInt(in.readLine()))
            {
            case 0:
               rankKnowledge[i] = RANK_KNOWLEDGE.DOES_HAVE;
               break;

            case 1:
               rankKnowledge[i] = RANK_KNOWLEDGE.DOES_NOT_HAVE;
               break;

            case 2:
               rankKnowledge[i] = RANK_KNOWLEDGE.MIGHT_HAVE;
               break;

            case 3:
               rankKnowledge[i] = RANK_KNOWLEDGE.CANNOT_HAVE;
               break;
            }
         }
         in.close();
      }
      catch (FileNotFoundException e) {}
      catch (IOException e)
      {
         Log.d("GoFish", "Error loading " + savePath);
      }
   }


   // Save game.
   void saveGame()
   {
      int i;

      try
      {
         PrintWriter out = new PrintWriter(new FileWriter(savePath));
         for (i = 0; i < 13; i++)
         {
            myHand[i].save(out);
         }
         for (i = 0; i < 13; i++)
         {
            otherHand[i].save(out);
         }
         out.println(myScoreRanks.size() + "");
         for (i = 0; i < myScoreRanks.size(); i++)
         {
            myScoreRanks.get(i).save(out);
         }
         out.println(otherScoreRanks.size() + "");
         for (i = 0; i < otherScoreRanks.size(); i++)
         {
            otherScoreRanks.get(i).save(out);
         }
         myStage.save(out);
         otherStage.save(out);
         exchange.save(out);
         for (i = 0; i < 52; i++)
         {
            deck[i].save(out);
         }
         out.println(deckDeal + "");
         out.println(askRank + "");
         out.println(myHandShift + "");
         switch (gameState)
         {
         case START:
            out.println("0");
            break;

         case MY_TURN:
            out.println("1");
            break;

         case MY_HAND_TO_STAGE_SHIFT:
            out.println("2");
            break;

         case MY_HAND_TO_STAGE_ONLY:
            out.println("3");
            break;

         case MY_STAGE_TO_HAND:
            out.println("4");
            break;

         case MY_STAGE_TO_EXCHANGE:
            out.println("5");
            break;

         case MY_STAGE_TO_SCORE:
            out.println("6");
            break;

         case EXCHANGE_TO_MY_STAGE:
            out.println("7");
            break;

         case OTHER_TURN:
            out.println("8");
            break;

         case OTHER_HAND_TO_STAGE:
            out.println("9");
            break;

         case OTHER_STAGE_TO_HAND:
            out.println("10");
            break;

         case OTHER_STAGE_TO_EXCHANGE:
            out.println("11");
            break;

         case OTHER_STAGE_TO_SCORE:
            out.println("12");
            break;

         case EXCHANGE_TO_OTHER_STAGE:
            out.println("13");
            break;

         case MY_DRAW_TO_EXCHANGE:
            out.println("14");
            break;

         case OTHER_DRAW_TO_EXCHANGE:
            out.println("15");
            break;

         case MY_WIN:
            out.println("16");
            break;

         case OTHER_WIN:
            out.println("17");
            break;

         case TIE:
            out.println("18");
            break;
         }
         out.println(myScore + "");
         out.println(otherScore + "");
         for (i = 0; i < 13; i++)
         {
            switch (rankKnowledge[i])
            {
            case DOES_HAVE:
               out.println("0");
               break;

            case DOES_NOT_HAVE:
               out.println("1");
               break;

            case MIGHT_HAVE:
               out.println("2");
               break;

            case CANNOT_HAVE:
               out.println("3");
               break;
            }
         }
         out.close();
      }
      catch (IOException e)
      {
         Log.d("GoFish", "Error saving " + savePath);
      }
   }
}
