package io.reitmaier.transcripttool.core.ui.player

import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ClippingMediaSource
import androidx.media3.exoplayer.source.ConcatenatingMediaSource
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.extractor.DefaultExtractorsFactory
import io.reitmaier.transcripttool.core.data.domain.PlayBackSpeed
import io.reitmaier.transcripttool.core.data.domain.PlaybackProgress
import io.reitmaier.transcripttool.core.data.domain.PlaybackState
import io.reitmaier.transcripttool.core.data.domain.Region
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import logcat.logcat
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.milliseconds

@UnstableApi
@Singleton
class ExoAudioPlayer @Inject constructor(
  private val context: Context,
) {
  private val _playbackState = MutableStateFlow<PlaybackState>(PlaybackState.Paused)
  val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

  private val _playbackProgress = MutableStateFlow(PlaybackProgress(0, 0, 0))
  val playbackProgress: StateFlow<PlaybackProgress> = _playbackProgress.asStateFlow()

  private val globalListener = object : Player.Listener {

    override fun onPlaybackStateChanged(playbackState: Int) {
      when (playbackState) {
        Player.STATE_BUFFERING -> _playbackState.value = PlaybackState.Loading
        Player.STATE_READY -> updatePlayerState()
        Player.STATE_IDLE, Player.STATE_ENDED -> {
          _playbackState.value = PlaybackState.Paused
//          exoPlayer.seekToDefaultPosition()
          exoPlayer.seekTo(0)
          exoPlayer.pause()
        }
      }
    }

    override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
      updatePlayerState()
    }

    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
      if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO) {
        logcat { "Pausing instead of transitioning to next Media Item: $reason ${mediaItem?.mediaMetadata}" }
        exoPlayer.pause()
        exoPlayer.seekToPreviousMediaItem()
      } else {
        logcat { "Transitioning to media item for different reason: $reason" }
      }
    }

    private fun updatePlayerState() {
      _playbackState.value = if (exoPlayer.isPlaying) {
        exoPlayer.setupPlaybackProgressTimer()
        PlaybackState.Playing
      } else {
        durationTimerJob?.cancel()
        PlaybackState.Paused
      }
    }
  }

  val dataSourceFactory = DefaultDataSource.Factory(
    context,
    DefaultHttpDataSource.Factory().setAllowCrossProtocolRedirects(true),
  )
  val mediaSourceFactory = DefaultMediaSourceFactory(
    dataSourceFactory,
    DefaultExtractorsFactory(),
  )
  val exoPlayer = ExoPlayer.Builder(context, mediaSourceFactory).build()

  init {
    exoPlayer.addListener(globalListener)
  }

  /**
   *  Playback states creation
   */
  private val defaultDataSourceFactory = DefaultDataSource.Factory(context).createDataSource()

  fun duration(): Long {
    return exoPlayer.duration
  }

  var speed: PlayBackSpeed = PlayBackSpeed.NORMAL
    set(value) {
      field = value
      exoPlayer.setPlaybackSpeed(value.value)
    }

  fun prepare(path: String) {
    val file = context.filesDir.resolve(path)
    val uri = Uri.fromFile(file)
    val mediaItem = MediaItem.fromUri(uri)
    val mediaSourceItem = mediaSourceFactory.createMediaSource(mediaItem)
//    val mediaSourceItem = buildRawMediaSource()
    exoPlayer.setMediaSource(mediaSourceItem)

    exoPlayer.prepare()
    exoPlayer.seekToDefaultPosition()
  }

  fun prepare(filename: String, regions: List<Region>, speed: PlayBackSpeed = PlayBackSpeed.NORMAL) {
    logcat { "Preparing media item $filename" }
    val file = context.filesDir.resolve(filename)
    val uri = Uri.fromFile(file)
    val mediaItem = MediaItem.fromUri(uri)
    val mediaSourceItem = mediaSourceFactory.createMediaSource(mediaItem)
//    val mediaSourceItem = buildRawMediaSource()
    val concatenatingMediaSource = ConcatenatingMediaSource()
    for (region in regions) {
      val clippingMediaSource = ClippingMediaSource(
        mediaSourceItem,
        region.start.milliseconds.inWholeMicroseconds,
        region.end.milliseconds.inWholeMicroseconds,
      )
      concatenatingMediaSource.addMediaSource(clippingMediaSource)
//      exoPlayer.addMediaSource(clippingMediaSource)
    }
    exoPlayer.setMediaSource(concatenatingMediaSource)

    exoPlayer.prepare()

    this.speed = speed
    exoPlayer.seekToDefaultPosition()
//    exoPlayer.play()
  }

  fun pause() {
    exoPlayer.pause()
  }

  fun resume() {
    exoPlayer.play()
  }

  fun playPause() {
    when (playbackState.value) {
      PlaybackState.Loading -> Unit
      PlaybackState.Paused -> exoPlayer.play()
      PlaybackState.Playing -> exoPlayer.pause()
    }
  }

  fun previous() {
    exoPlayer.seekToPreviousMediaItem()
//    exoPlayer.play()
  }

  fun next() {
    exoPlayer.seekToNextMediaItem()
//    exoPlayer.play()
  }

  fun seekTo(position: Long) {
    exoPlayer.seekTo(position)
  }

  fun clear() {
    exoPlayer.clearMediaItems()
  }

  /**
   *  Playback timer
   */
  private var durationTimerJob: Job? = null

  private fun Player.setupPlaybackProgressTimer() {
    durationTimerJob?.cancel()
    // TODO coroutine inject scope
    durationTimerJob = GlobalScope.launch {
      while (true) {
        withContext(Dispatchers.Main) {
          _playbackProgress.value = PlaybackProgress(
            current = currentPosition,
            regionPosition = currentMediaItemIndex,
            duration = duration.coerceAtLeast(0),
          )
        }
        delay(50)
      }
    }
  }

  fun seekForward() {
    exoPlayer.seekForward()
  }

  fun seekBack() {
    exoPlayer.seekBack()
  }
}
