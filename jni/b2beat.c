/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 * File was modified 2013 by Thomas Baag
 */

#include <assert.h>
#include <jni.h>
#include <string.h>

// for __android_log_print(ANDROID_LOG_INFO, "YourApp", "formatted message");
//#include <android/log.h>

// for native audio
#include <SLES/OpenSLES.h>
#include <SLES/OpenSLES_Android.h>

// for native asset manager
#include <sys/types.h>
#include <android/asset_manager.h>
#include <android/asset_manager_jni.h>

// engine interfaces
static SLObjectItf engineObject = NULL;
static SLEngineItf engineEngine;

// output mix interfaces
static SLObjectItf outputMixObject = NULL;
static SLEnvironmentalReverbItf outputMixEnvironmentalReverb = NULL;

// file descriptor player interfaces
#define MAX_SOUNDS 30
static SLObjectItf fdPlayerObject[MAX_SOUNDS] = { NULL };
static SLPlayItf fdPlayerPlay[MAX_SOUNDS];
static SLSeekItf fdPlayerSeek[MAX_SOUNDS];
static SLMuteSoloItf fdPlayerMuteSolo[MAX_SOUNDS];
static SLVolumeItf fdPlayerVolume[MAX_SOUNDS];

// create the engine and output mix objects
void Java_de_b2ag_b2beat_MainActivity_createEngine( JNIEnv* env, jclass clazz )
{
  SLresult result;

  // create engine
  result = slCreateEngine( &engineObject, 0, NULL, 0, NULL, NULL );
  assert( SL_RESULT_SUCCESS == result );

  // realize the engine
  result = ( *engineObject )->Realize( engineObject, SL_BOOLEAN_FALSE );
  assert( SL_RESULT_SUCCESS == result );

  // get the engine interface, which is needed in order to create other objects
  result = ( *engineObject )->GetInterface( engineObject, SL_IID_ENGINE, &engineEngine );
  assert( SL_RESULT_SUCCESS == result );

  // create output mix
  result = ( *engineEngine )->CreateOutputMix( engineEngine, &outputMixObject, 0, NULL, NULL );
  assert( SL_RESULT_SUCCESS == result );

  // realize the output mix
  result = ( *outputMixObject )->Realize( outputMixObject, SL_BOOLEAN_FALSE );
  assert( SL_RESULT_SUCCESS == result );

}

void b2beat_rewind( SLPlayItf caller, void *pContext, SLuint32 event )
{
  const int id = (int) pContext;
  SLresult result;

  // set the player's state
  result = ( *fdPlayerPlay[id] )->SetPlayState( fdPlayerPlay[id], SL_PLAYSTATE_STOPPED );
  assert( SL_RESULT_SUCCESS == result );

  result = ( *fdPlayerPlay[id] )->SetPlayState( fdPlayerPlay[id], SL_PLAYSTATE_PAUSED );
  assert( SL_RESULT_SUCCESS == result );

  // seek to beginning
  result = ( *fdPlayerSeek[id] )->SetPosition( fdPlayerSeek[id], 0, SL_SEEKMODE_FAST );
  assert( SL_RESULT_SUCCESS == result );

}

// create asset audio player
jboolean Java_de_b2ag_b2beat_MainActivity_createAssetAudioPlayer( JNIEnv* env, jclass clazz, jobject assetManager, jint id, jstring filename )
{
  SLresult result;

  if ( fdPlayerObject[id] != NULL )
  {
    ( *fdPlayerObject[id] )->Destroy( fdPlayerObject[id] );
    fdPlayerObject[id] = NULL;
  }

  if ( id >= MAX_SOUNDS )
    return JNI_FALSE;

  // convert Java string to UTF-8
  const char *utf8 = ( *env )->GetStringUTFChars( env, filename, NULL );
  assert( NULL != utf8 );

  // use asset manager to open asset by filename
  AAssetManager* mgr = AAssetManager_fromJava( env, assetManager );
  assert( NULL != mgr );
  AAsset* asset = AAssetManager_open( mgr, utf8, AASSET_MODE_UNKNOWN );

  // release the Java string and UTF-8
  ( *env )->ReleaseStringUTFChars( env, filename, utf8 );

  // the asset might not be found
  if ( NULL == asset )
  {
    return JNI_FALSE;
  }

  // open asset as file descriptor
  off_t start, length;
  int fd = AAsset_openFileDescriptor( asset, &start, &length );
  assert( 0 <= fd );
  AAsset_close( asset );

  // configure audio source
  SLDataLocator_AndroidFD loc_fd = { SL_DATALOCATOR_ANDROIDFD, fd, start, length };
  SLDataFormat_MIME format_mime = { SL_DATAFORMAT_MIME, NULL, SL_CONTAINERTYPE_UNSPECIFIED };
  SLDataSource audioSrc = { &loc_fd, &format_mime };

  // configure audio sink
  SLDataLocator_OutputMix loc_outmix = { SL_DATALOCATOR_OUTPUTMIX, outputMixObject };
  SLDataSink audioSnk = { &loc_outmix, NULL };

  // create audio player
  const SLInterfaceID ids[3] = { SL_IID_SEEK, SL_IID_MUTESOLO, SL_IID_VOLUME };
  const SLboolean req[3] = { SL_BOOLEAN_TRUE, SL_BOOLEAN_TRUE, SL_BOOLEAN_TRUE };
  result = ( *engineEngine )->CreateAudioPlayer( engineEngine, &fdPlayerObject[id], &audioSrc, &audioSnk, 3, ids, req );
  assert( SL_RESULT_SUCCESS == result );

  // realize the player
  result = ( *fdPlayerObject[id] )->Realize( fdPlayerObject[id], SL_BOOLEAN_FALSE );
  assert( SL_RESULT_SUCCESS == result );

  // get the play interface
  result = ( *fdPlayerObject[id] )->GetInterface( fdPlayerObject[id], SL_IID_PLAY, &fdPlayerPlay[id] );
  assert( SL_RESULT_SUCCESS == result );

  result = ( *fdPlayerPlay[id] )->RegisterCallback( fdPlayerPlay[id], b2beat_rewind, (void*) id );
  assert( SL_RESULT_SUCCESS == result );
  result = ( *fdPlayerPlay[id] )->SetCallbackEventsMask( fdPlayerPlay[id], SL_PLAYEVENT_HEADATEND );
  assert( SL_RESULT_SUCCESS == result );

  // prefetch
  result = ( *fdPlayerPlay[id] )->SetPlayState( fdPlayerPlay[id], SL_PLAYSTATE_PAUSED );
  assert( SL_RESULT_SUCCESS == result );

  // get the seek interface
  result = ( *fdPlayerObject[id] )->GetInterface( fdPlayerObject[id], SL_IID_SEEK, &fdPlayerSeek[id] );
  assert( SL_RESULT_SUCCESS == result );

  // get the mute/solo interface
  result = ( *fdPlayerObject[id] )->GetInterface( fdPlayerObject[id], SL_IID_MUTESOLO, &fdPlayerMuteSolo[id] );
  assert( SL_RESULT_SUCCESS == result );

  // get the volume interface
  result = ( *fdPlayerObject[id] )->GetInterface( fdPlayerObject[id], SL_IID_VOLUME, &fdPlayerVolume[id] );
  assert( SL_RESULT_SUCCESS == result );

  /*
   // enable whole file looping
   result = ( *fdPlayerSeek[id] )->SetLoop( fdPlayerSeek[id], SL_BOOLEAN_TRUE, 0, SL_TIME_UNKNOWN );
   assert( SL_RESULT_SUCCESS == result );
   */

  return JNI_TRUE;
}

// create asset audio player
jboolean Java_de_b2ag_b2beat_MainActivity_createUriAudioPlayer( JNIEnv* env, jclass clazz, jint id, jstring uri )
{
  SLresult result;

  if ( fdPlayerObject[id] != NULL )
  {
    ( *fdPlayerObject[id] )->Destroy( fdPlayerObject[id] );
    fdPlayerObject[id] = NULL;
  }

  if ( id >= MAX_SOUNDS )
    return JNI_FALSE;

  // convert Java string to UTF-8
  const char *utf8 = ( *env )->GetStringUTFChars( env, uri, NULL );
  assert( NULL != utf8 );

  // configure audio source
  // (requires the INTERNET permission depending on the uri parameter)
  SLDataLocator_URI loc_uri = { SL_DATALOCATOR_URI, (SLchar *) utf8 };
  SLDataFormat_MIME format_mime = { SL_DATAFORMAT_MIME, NULL, SL_CONTAINERTYPE_UNSPECIFIED };
  SLDataSource audioSrc = { &loc_uri, &format_mime };

  // configure audio sink
  SLDataLocator_OutputMix loc_outmix = { SL_DATALOCATOR_OUTPUTMIX, outputMixObject };
  SLDataSink audioSnk = { &loc_outmix, NULL };

  // create audio player
  const SLInterfaceID ids[3] = { SL_IID_SEEK, SL_IID_MUTESOLO, SL_IID_VOLUME };
  const SLboolean req[3] = { SL_BOOLEAN_TRUE, SL_BOOLEAN_TRUE, SL_BOOLEAN_TRUE };
  result = ( *engineEngine )->CreateAudioPlayer( engineEngine, &fdPlayerObject[id], &audioSrc, &audioSnk, 3, ids, req );
  // note that an invalid URI is not detected here, but during prepare/prefetch on Android,
  // or possibly during Realize on other platforms
  assert( SL_RESULT_SUCCESS == result );

  // release the Java string and UTF-8
  ( *env )->ReleaseStringUTFChars( env, uri, utf8 );

  // realize the player
  result = ( *fdPlayerObject[id] )->Realize( fdPlayerObject[id], SL_BOOLEAN_FALSE );
  assert( SL_RESULT_SUCCESS == result );

  // get the play interface
  result = ( *fdPlayerObject[id] )->GetInterface( fdPlayerObject[id], SL_IID_PLAY, &fdPlayerPlay[id] );
  assert( SL_RESULT_SUCCESS == result );

  result = ( *fdPlayerPlay[id] )->RegisterCallback( fdPlayerPlay[id], b2beat_rewind, (void*) id );
  assert( SL_RESULT_SUCCESS == result );
  result = ( *fdPlayerPlay[id] )->SetCallbackEventsMask( fdPlayerPlay[id], SL_PLAYEVENT_HEADATEND );
  assert( SL_RESULT_SUCCESS == result );

  // prefetch
  result = ( *fdPlayerPlay[id] )->SetPlayState( fdPlayerPlay[id], SL_PLAYSTATE_PAUSED );
  assert( SL_RESULT_SUCCESS == result );

  // get the seek interface
  result = ( *fdPlayerObject[id] )->GetInterface( fdPlayerObject[id], SL_IID_SEEK, &fdPlayerSeek[id] );
  assert( SL_RESULT_SUCCESS == result );

  // get the mute/solo interface
  result = ( *fdPlayerObject[id] )->GetInterface( fdPlayerObject[id], SL_IID_MUTESOLO, &fdPlayerMuteSolo[id] );
  assert( SL_RESULT_SUCCESS == result );

  // get the volume interface
  result = ( *fdPlayerObject[id] )->GetInterface( fdPlayerObject[id], SL_IID_VOLUME, &fdPlayerVolume[id] );
  assert( SL_RESULT_SUCCESS == result );

  /*
   // enable whole file looping
   result = ( *fdPlayerSeek[id] )->SetLoop( fdPlayerSeek[id], SL_BOOLEAN_TRUE, 0, SL_TIME_UNKNOWN );
   assert( SL_RESULT_SUCCESS == result );
   */

  return JNI_TRUE;
}
// set the playing state for the asset audio player
void Java_de_b2ag_b2beat_MainActivity_setPlayingAudioPlayer( JNIEnv* env, jclass clazz, jint id )
{
  SLresult result;

  // make sure the asset audio player was created
  if ( NULL != fdPlayerPlay[id] )
  {
    // seek to beginning
    result = ( *fdPlayerSeek[id] )->SetPosition( fdPlayerSeek[id], 0, SL_SEEKMODE_FAST );
    assert( SL_RESULT_SUCCESS == result );
    // set the player's state
    result = ( *fdPlayerPlay[id] )->SetPlayState( fdPlayerPlay[id], SL_PLAYSTATE_PLAYING );
    assert( SL_RESULT_SUCCESS == result );
  }

}

// set the playing state for the asset audio player
void Java_de_b2ag_b2beat_MainActivity_setStoppedAudioPlayer( JNIEnv* env, jclass clazz, jint id )
{
  SLresult result;

  // make sure the asset audio player was created
  if ( NULL != fdPlayerPlay[id] )
  {
    // set the player's state
    result = ( *fdPlayerPlay[id] )->SetPlayState( fdPlayerPlay[id], SL_PLAYSTATE_STOPPED );
    assert( SL_RESULT_SUCCESS == result );

    result = ( *fdPlayerPlay[id] )->SetPlayState( fdPlayerPlay[id], SL_PLAYSTATE_PAUSED );
    assert( SL_RESULT_SUCCESS == result );

    // seek to beginning
    result = ( *fdPlayerSeek[id] )->SetPosition( fdPlayerSeek[id], 0, SL_SEEKMODE_FAST );
    assert( SL_RESULT_SUCCESS == result );
  }

}

// shut down the native audio system
void Java_de_b2ag_b2beat_MainActivity_shutdown( JNIEnv* env, jclass clazz )
{
  int id;
  // destroy file descriptor audio player object, and invalidate all associated interfaces
  for ( id = 0; id < MAX_SOUNDS; id++ )
  {
    if ( fdPlayerObject[id] != NULL )
    {
      ( *fdPlayerObject[id] )->Destroy( fdPlayerObject[id] );
      fdPlayerObject[id] = NULL;
    }
  }
  // destroy output mix object, and invalidate all associated interfaces
  if ( outputMixObject != NULL )
  {
    ( *outputMixObject )->Destroy( outputMixObject );
    outputMixObject = NULL;
    outputMixEnvironmentalReverb = NULL;
  }

  // destroy engine object, and invalidate all associated interfaces
  if ( engineObject != NULL )
  {
    ( *engineObject )->Destroy( engineObject );
    engineObject = NULL;
    engineEngine = NULL;
  }

}
