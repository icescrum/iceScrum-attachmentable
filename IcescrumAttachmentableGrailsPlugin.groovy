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

import org.icescrum.plugins.attachmentable.domain.Attachment
import org.icescrum.plugins.attachmentable.domain.AttachmentLink
import org.icescrum.plugins.attachmentable.interfaces.Attachmentable
import org.icescrum.plugins.attachmentable.services.AttachmentableService

class IcescrumAttachmentableGrailsPlugin {
    def groupId = "org.icescrum"
    def version = "1.0.7"
    def grailsVersion = "2.5 > *"
    def author = "Vincent Barrier"
    def authorEmail = "vincent.barrier@icescrum.com"
    def title = "iceScrum attachmentable plugin"
    def description = '''Attach file to your domain class in a generic manner (without MultipartFile or Ajax)
           if you don't need to customize ajax/multipartFile -> Try Attachmentable plugin'''

    def doWithSpring = {
        attachmentsBaseDir application
    }

    def doWithDynamicMethods = { ctx ->
        AttachmentableService attachmentableService = ctx.getBean('attachmentableService')
        for (domainClass in application.domainClasses) {
            if (Attachmentable.class.isAssignableFrom(domainClass.clazz)) {
                domainClass.clazz.metaClass {
                    addAttachment { poster, def file, String originalName = null ->
                        return attachmentableService.addAttachment(poster, delegate, file, originalName)
                    }
                    addAttachments { poster, def tmpFiles ->
                        tmpFiles.each { tmpFile ->
                            if (tmpFile instanceof File) {
                                addAttachment(poster, tmpFile)
                            } else {
                                addAttachment(poster, tmpFile.url ? tmpFile : tmpFile.file, tmpFile.filename)
                            }
                        }
                    }
                    removeAttachment { Attachment attachment ->
                        attachmentableService.removeAttachment(attachment, delegate)
                    }
                    removeAttachment { Long id ->
                        def attachment = Attachment.load(id)
                        if (attachment) {
                            removeAttachment(attachment)
                        }
                    }
                    removeAllAttachments {
                        def delDir = delegate.attachments?.findAll { it.url != null }?.size() > 0 ?: false
                        delegate.attachments?.each { Attachment a ->
                            removeAttachment(a)
                        }
                        if (delDir) {
                            attachmentableService.removeAttachmentDir(delegate)
                        }
                    }
                    getAttachments = { ->
                        AttachmentLink.getAttachments(delegate).list()
                    }
                    getTotalAttachments = { ->
                        AttachmentLink.getTotalAttachments(delegate).list()[0]
                    }
                }
            }
        }
    }

    private void attachmentsBaseDir(application) {
        def dir = application.config.grails.attachmentable?.baseDir
        if (!dir) {
            String userHome = System.properties.'user.home'
            String appName = application.metadata['app.name']
            dir = new File(userHome, appName).canonicalPath
            application.config.grails.attachmentable.baseDir = dir
        }
    }
}
