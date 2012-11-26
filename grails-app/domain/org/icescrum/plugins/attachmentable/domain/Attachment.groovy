package org.icescrum.plugins.attachmentable.domain
class Attachment implements Serializable {

    // file
    String name
    String ext
    String contentType
    Long length
    Date dateCreated

    //For generic cloud storage
    String url
    String provider

    // poster
    String posterClass
    Long posterId

    // input name
    String inputName

    static constraints = {
        name nullable: false, blank: false
        ext nullable: true, blank: true
        inputName nullable:false, blank: false
        contentType nullable: true, blank: true
        length min: 0L
        posterClass blank: false
        posterId min: 0L
        url(maxSize: 1000, nullable: true)
        provider nullable:true
    }

    static mapping = {
        cache true
        url length: 1000
        table 'attachmentable_attachment'
    }

    String toString() {
        filename
    }

    def getFilename() {
        ext ? "$name.$ext" : "$name"
    }

    def getPoster() {
        // handle proxied class names
        def i = posterClass.indexOf('_$$_javassist')
        if (i > -1)
            posterClass = posterClass[0..i - 1]
        getClass().classLoader.loadClass(posterClass).get(posterId)
    }
}