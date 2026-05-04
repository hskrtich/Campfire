package app.campfire.audioplayer.impl

import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.extractor.DefaultExtractorsFactory
import androidx.media3.extractor.Extractor
import androidx.media3.extractor.ExtractorsFactory

/**
 * Wraps [DefaultExtractorsFactory] to fix xHE-AAC playback for files stored with a `.aac`
 * extension inside an MP4 container.
 *
 * ExoPlayer's [DefaultExtractorsFactory] orders extractors by URI extension — `.aac` causes
 * [androidx.media3.extractor.ts.AdtsExtractor] to be tried before
 * [androidx.media3.extractor.mp4.Mp4Extractor]. xHE-AAC (USAC, audio object type 42) in an MP4
 * container is not parseable by [androidx.media3.extractor.ts.AdtsExtractor], which throws
 * "Invalid AAC audio". By substituting a `.mp4` extension hint, [Mp4Extractor] is tried first.
 * For genuine ADTS content, [Mp4Extractor.sniff] returns false and [AdtsExtractor] is still used.
 */
@OptIn(UnstableApi::class)
internal class XheAacCompatExtractorsFactory(
  private val delegate: DefaultExtractorsFactory,
) : ExtractorsFactory {

  override fun createExtractors(): Array<Extractor> = delegate.createExtractors()

  override fun createExtractors(
    uri: Uri,
    responseHeaders: Map<String, List<String>>,
  ): Array<Extractor> {
    val effectiveUri = uri.path
      ?.takeIf { uri.lastPathSegment?.endsWith(".aac", ignoreCase = true) == true }
      ?.let { path -> uri.buildUpon().path(path.dropLast(".aac".length) + ".mp4").build() }
      ?: uri
    return delegate.createExtractors(effectiveUri, responseHeaders)
  }
}
