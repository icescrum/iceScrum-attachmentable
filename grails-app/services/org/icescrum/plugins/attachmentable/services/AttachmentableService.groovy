/*
 * Copyright (c) 2010 iceScrum Technologies.
 *
 * This file is part of iceScrum.
 *
 * iceScrum is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License.
 *
 * iceScrum is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with iceScrum.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Authors:
 *
 * Vincent Barrier (vincent.barrier@icescrum.com)
 *
 */

package org.icescrum.plugins.attachmentable.services

import grails.util.GrailsNameUtils
import grails.util.Holders
import org.apache.commons.io.FileUtils
import org.apache.commons.io.FilenameUtils
import org.codehaus.groovy.grails.commons.GrailsClassUtils
import org.codehaus.groovy.grails.web.context.ServletContextHolder as SCH
import org.icescrum.plugins.attachmentable.domain.Attachment
import org.icescrum.plugins.attachmentable.domain.AttachmentLink

class AttachmentableService {

    def addAttachment(def poster, def delegate, def file, def originalName = null) {

        if (delegate.id == null) {
            throw new RuntimeException("You must save the entity [${delegate}] before calling addAttachment")
        }

        def posterClass = poster.class.name
        def i = posterClass.indexOf('_$$_javassist')
        if (i > -1) {
            posterClass = posterClass[0..i - 1]
        }

        if (file instanceof File && !file?.length()) {
            throw new RuntimeException("Error file : ${file.getName()} is empty (${file.getAbsolutePath()})")
        }

        String filename = originalName ?: file.name

        def attachment = new Attachment(posterId: poster.id,
                posterClass: posterClass,
                inputName: originalName ?: file.name,
                name: FilenameUtils.getBaseName(filename),
                ext: FilenameUtils.getExtension(filename),
                length: file instanceof File ? file.length() : file.length,
                url: file instanceof Map ? file.url : null,
                provider: file instanceof Map ? file.provider : null,
                contentType: file instanceof File ? SCH.servletContext.getMimeType(filename.toLowerCase()) : null)

        if (!attachment.validate()) {
            throw new RuntimeException("Cannot create attachment for arguments [$poster, $file], they are invalid.")
        }
        attachment.save()

        def delegateClass = delegate.class.name
        i = delegateClass.indexOf('_$$_javassist')
        if (i > -1) {
            delegateClass = delegateClass[0..i - 1]
        }

        def link = new AttachmentLink(attachment: attachment, attachmentRef: delegate.id, type: GrailsNameUtils.getPropertyName(delegate.class), attachmentRefClass: delegateClass)
        link.save()

        if (file instanceof File) {
            // Save the file on disk
            def diskFile = new File(getFileDir(delegate), "${attachment.id + (attachment.ext ? '.' + attachment.ext : '')}")
            FileUtils.moveFile(file, diskFile)
            try {
                delegate.onAddAttachment(attachment)
            } catch (MissingMethodException e) {}
        }
        return delegate
    }

    def removeAttachment(Attachment attachment, def delegate) {
        if (!attachment.url) {
            def diskFile = new File(getFileDir(delegate), "${attachment.id + (attachment.ext ? '.' + attachment.ext : '')}")
            diskFile.delete()
            try {
                delegate.onRemoveAttachment(attachment)
            } catch (MissingMethodException e) {}
        }
        return delegate
    }

    def removeAttachmentDir(def delegate) {
        getFileDir(delegate).deleteDir()
        return delegate
    }

    private getFileDir(def object) {
        def dir = Holders.config.grails.attachmentable.baseDir
        if (Holders.config.grails.attachmentable?."${GrailsClassUtils.getShortName(object.class).toLowerCase()}Dir") {
            dir = "${dir}${Holders.config.grails.attachmentable?."${GrailsClassUtils.getShortName(object.class).toLowerCase()}Dir"(object)}"
        }
        def fileDir = new File(dir)
        fileDir.mkdirs()
        return fileDir
    }

    def getFile(def attachment) {
        def link = AttachmentLink.findByAttachment(attachment)
        def delegate = getClass().classLoader.loadClass(link.attachmentRefClass).get(link.attachmentRef)
        def diskFile = new File(getFileDir(delegate), "${attachment.id + (attachment.ext ? '.' + attachment.ext : '')}")
        if (!diskFile) {
            throw new FileNotFoundException()
        }
        return diskFile
    }
}
