package com.android.settings;

import android.media.audiofx.AudioEffect;
import java.util.UUID;

public class AudioEnhancer
  extends AudioEffect
{
  private static final UUID EFFECT_TYPE_AUDIOENHANCER = UUID.fromString("e069d9e0-8329-11df-9168-0002a5d5c51b");

  public AudioEnhancer(int paramInt1, int paramInt2)
  {
    super(EFFECT_TYPE_NULL, EFFECT_TYPE_AUDIOENHANCER, paramInt1, paramInt2);
  }

  public int getHeadsetType()
    throws IllegalStateException, IllegalArgumentException, UnsupportedOperationException
  {
    int[] arrayOfInt = new int[1];
    checkStatus(getParameter(1, arrayOfInt));
    return arrayOfInt[0];
  }


  public int getMode()
    throws IllegalStateException, IllegalArgumentException, UnsupportedOperationException
  {
    int[] arrayOfInt = new int[1];
    checkStatus(getParameter(3, arrayOfInt));
    return arrayOfInt[0];
  }

  public int getMusic()
    throws IllegalStateException, IllegalArgumentException, UnsupportedOperationException
  {
    int[] arrayOfInt = new int[1];
    checkStatus(getParameter(4, arrayOfInt));
    return arrayOfInt[0];
  }

  public void setHeadsetType(int paramInt)
    throws IllegalStateException, IllegalArgumentException, UnsupportedOperationException
  {
    checkStatus(setParameter(1, paramInt));
  }


  public void setMode(int paramInt)
    throws IllegalStateException, IllegalArgumentException, UnsupportedOperationException
  {
    checkStatus(setParameter(3, paramInt));
  }

  public void setMusic(int paramInt)
    throws IllegalStateException, IllegalArgumentException, UnsupportedOperationException
  {
    checkStatus(setParameter(4, paramInt));
  }

}

