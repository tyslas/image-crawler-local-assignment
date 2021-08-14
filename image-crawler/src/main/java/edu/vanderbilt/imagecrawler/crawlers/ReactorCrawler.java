package edu.vanderbilt.imagecrawler.crawlers;

import static edu.vanderbilt.imagecrawler.utils.Crawler.Type.PAGE;

import java.net.URL;
import java.util.Optional;
import java.util.concurrent.ForkJoinPool;

import edu.vanderbilt.imagecrawler.utils.Crawler;
import edu.vanderbilt.imagecrawler.utils.Image;
import edu.vanderbilt.imagecrawler.utils.Options;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * This class uses Project Reactor Flux and Mono reactive types to
 * perform a concurrent image crawl starting from a root Uri.  Images
 * from HTML pages reachable from the root Uri are downloaded from a
 * remote web server or from the local file system, transformed, and
 * then stored in files that can be displayed to the user.
 */
public class ReactorCrawler
       extends ImageCrawler {
    /**
     * Recursively crawls the given page and returns the total number
     * of processed images.
     *
     * @param pageUri The URI that's being crawled at this point
     * @param depth   The current depth of the recursive processing
     * @return The number of images processed at this depth
     */
    @Override
    protected int performCrawl(String pageUri, int depth) {
        // Call the crawlPageAsync() helper method to perform the
        // crawl asynchronously, obtain a count of the number of
        // images downloaded/processed, wait until all the processing
        // is done, and return the result as an int value.  If any
        // error/exception is encountered return 0.

        // TODO -- you fill in here replacing this statement with your
        // solution.
        // Crawl the page using the specified uri and depth.
        return crawlPageAsync(pageUri, depth)
                // Count the number of elements in the Flux stream.
                .count()
                // Return 0 if any exception is encountered.
                .onErrorReturn(0L)
                // Block to get the result.
                .block()
                // Convert the result to an int primitive.
                .intValue();
    }

    /**
     * Recursively crawls the web page identified by the passed {@code
     * pageUri} and downloads and transforms all discovered images.
     * The recursion ends when the passed depth is exceeded.
     *
     * @param pageUri The uri to crawl
     * @param depth   The maximum depth of the crawl
     * @return A {@link Flux} that emits transformed images
     */
    protected Flux<Image> crawlPageAsync(String pageUri, int depth) {
        // Return a Flux stream of images from this page and all page
        // links recursively reachable from it.  Return an empty
        // Flux if the depth limit of the web crawling is
        // reached or if the pageUri has already been visited.  This
        // method should use Flux operators like just(), filter(),
        // map(), and flatMap() (this latter operator should call
        // imagesOnPageAndPageLinksAsync()).

        // TODO -- you fill in here replacing this statement with your
        // solution.
//        Mono<String> pageMono = Mono.fromCallable(Options::getRootUrlLocator)
//                .transformDeferred(
//                        mono -> mono.subscribeOn(
//                                Schedulers.fromExecutor(ForkJoinPool.commonPool()))
//                );
        // Create an Flux that emits this pageUri.
        Flux<String> pageFlux = Flux.from(Mono.fromCallable(() -> pageUri));
        return Flux.just(pageFlux)
                // Filter out page if it exceeds the depth or has already
                // been visited.
                .filter(uri -> depth > mMaxDepth || mUniqueUris.putIfAbsent(uri.toString()))
                // Map the url to a page.
                .map(uri -> mWebPageCrawler.getPage(uri.toString()))
                // Apply the flatMap() concurrency idiom to convert each
                // page to a Flux stream of images asynchronously.
                .flatMap(page -> imagesOnPageAndPageLinksAsync(page, depth));
    }

    /**
     * Returns all images on the page and its linked pages.
     *
     * @param page  The crawler page to crawl
     * @param depth The maximum depth of the crawl
     * @return A {@link Flux} that emits all images on the page and its linked pages
     */
    protected Flux<Image> imagesOnPageAndPageLinksAsync(Crawler.Page page,
                                                        int depth) {
        // Return a Flux stream consisting of all images on this page
        // and any images linked on it.  This method should call
        // imagesOnPageLinks() and imagesOnPage() and use Flux
        // operators mergeWith() and subscribeOn() to merge all the
        // images into a single Flux stream via a parallel Scheduler.

        // TODO -- you fill in here replacing this statement with your
        // solution.
        return imagesOnPageAsync(page)
                .mergeWith(imagesOnPageLinksAsync(page, depth))
                .subscribeOn(Schedulers.parallel());
    }

    /**
     * Find all page links on the passed page and recursively crawl
     * each page to download and transform all discovered images.
     *
     * @param page The page to search for page links
     * @return A {@link Flux} that emits non-null image transforms
     */
    protected Flux<Image> imagesOnPageLinksAsync(Crawler.Page page,
                                                 int depth) {
        // Create and return a Flux stream consisting of images that
        // have been downloaded and transformed.  This method should
        // call crawlPageAsync() recursively and use Flux operators
        // like fromIterable(), flatMap(), and subscribeOn() to
        // perform the processing on the parallel Scheduler.

        // TODO -- you fill in here replacing this statement with your
        // solution.
        // Convert the list of page links into an Flux
        // stream of page links.
        return Flux.fromIterable(page.getPageElementsAsStrings(PAGE))
                // Apply the flatMap() concurrency idiom to map each page
                // to a stream of images that are downloaded and
                // transformed concurrently via the parallel scheduler.
                .flatMap(s -> crawlPageAsync(s, depth));
    }

    /**
     * Find, download, and transform all images on the given page.
     *
     * @param page The page to search for image urls
     * @return A {@link Flux} that emits image transforms
     */
    protected Flux<Image> imagesOnPageAsync(Crawler.Page page) {
        // Create and return a Flux stream for this page using the
        // Flux flatMap() concurrency idiom.  It should call helper
        // methods getPageElementsAsUrls(), downloadImageAsync(), and
        // transformImageAsync(), as well as use Flux operators like
        // flatmap() and fromIterable() perform processing on the
        // parallel scheduler.  The flatMap() operator can be called
        // more than once.

        // TODO -- you fill in here replacing this statement with your
        // solution.
        // Convert the list of images in the page into an
        // Flux stream.
        return Flux.fromIterable(page.getPageElementsAsUrls(Crawler.Type.IMAGE))
                // Apply the flatMap() concurrency idiom to download
                // the stream of images in parallel.
                .flatMap(this::downloadImageAsync)
                // Again apply the flatMap() concurrency idiom to convert
                // the stream of downloaded images into a stream of images
                // are transformed in parallel.
                .flatMap(this::transformImageAsync);
    }

    /**
     * Asynchronously download and return the image at the given
     * {@code url}
     *
     * @param url The URL to download
     * @return A {@link Mono} that emits a single downloaded image
     */
    protected Mono<Image> downloadImageAsync(URL url) {
        // Create and return a Mono instance that emits the download
        // image at the specified URL. It should call the helper
        // method getOrDownloadImage(), as well as use Mono operators
        // like fromCallable(), map(), filter(), and subscribeOn() to
        // perform processing on the parallel scheduler.  The map()
        // operator can be called more than once.  In addition, Java
        // Optional methods (such as ofNullable(), isPresent(), and
        // get()) should be used to avoid dealing with null values.

        // TODO -- you fill in here replacing this statement with your
        // solution.
            // Create an Flux that emits the URL.
        return null;
//        return Flux.from(Mono.fromCallable(() -> url))
//                // Run computation in the parallel scheduler.
//                .subscribeOn(Schedulers.parallel())
//                // Map the image URL to a possibly downloaded image.
//                .map(url1 -> mImageCache.getItem(url1.getPath(), null))
//                .onErrorStop()
//                // Only continue processing if an image was available.
//                .flatMap(Optional::ofNullable)
//                // Convert optionals into values.
//                .map(Mono::just);
    }

    /**
     * Applies the current set of crawler transforms on the passed
     * image and returns a {@link Flux} stream of all successfully
     * transformed images.
     *
     * @param image The image to transform
     * @return A {@link Flux} that emits non-null transformed images
     */
    protected Flux<Image> transformImageAsync(Image image) {
        // Return a Flux stream of transformed images.  This method
        // should call the createNewCacheItem() and applyTransform()
        // helper methods, as well as use Flux operators like
        // fromIterable(), filter(), and map() (filter() and map() can
        // be called more than once).  In addition, Java Optional
        // methods (such as ofNullable(), isPresent(), and get())
        // should be used to avoid dealing with null values.

        // TODO -- you fill in here replacing this statement with your
        // solution.
        return null;
            // Convert the List of transforms into an stream of
            // transforms.


            // Run computations in the parallel scheduler.


            // Only transform images that haven't already been
            // transformed.


            // Apply the transform on the image.


            // Filter out any null returns from applyTransform().


            // Convert optionals into values.

    }
}
