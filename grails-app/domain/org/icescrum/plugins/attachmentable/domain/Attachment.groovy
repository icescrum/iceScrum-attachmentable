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

    static transients = ['previewable']

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

    def getPreviewable() {
        return this.contentType?.contains('image/') && this.url == null
    }

    def getPoster() {
        // handle proxied class names
        def i = posterClass.indexOf('_$$_javassist')
        if (i > -1)
            posterClass = posterClass[0..i - 1]
        getClass().classLoader.loadClass(posterClass).get(posterId)
    }

    def xml(builder) {
        builder.attachment(){
            ext(this.ext)
            length(this.length)
            posterId(this.posterId)
            posterClass(this.posterClass)
            dateCreated(this.dateCreated)
            url { builder.mkp.yieldUnescaped("<![CDATA[${this.url}]]>") }
            name { builder.mkp.yieldUnescaped("<![CDATA[${this.name}]]>") }
            provider { builder.mkp.yieldUnescaped("<![CDATA[${this.provider}]]>") }
            inputName { builder.mkp.yieldUnescaped("<![CDATA[${this.inputName}]]>") }
            contentType { builder.mkp.yieldUnescaped("<![CDATA[${this.contentType}]]>") }
        }
    }
}