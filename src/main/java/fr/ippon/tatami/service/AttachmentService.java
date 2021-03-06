package fr.ippon.tatami.service;

import fr.ippon.tatami.domain.Attachment;
import fr.ippon.tatami.domain.DomainConfiguration;
import fr.ippon.tatami.domain.User;
import fr.ippon.tatami.repository.AttachmentRepository;
import fr.ippon.tatami.repository.DomainConfigurationRepository;
import fr.ippon.tatami.repository.UserAttachmentRepository;
import fr.ippon.tatami.repository.UserRepository;
import fr.ippon.tatami.security.AuthenticationService;
import fr.ippon.tatami.service.exception.StorageSizeException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import javax.validation.ConstraintViolationException;
import java.util.Collection;

@Service
public class AttachmentService {

    private static final Log log = LogFactory.getLog(AttachmentService.class);

    @Inject
    private AttachmentRepository attachmentRepository;

    @Inject
    private UserAttachmentRepository userAttachmentRepository;

    @Inject
    private DomainConfigurationRepository domainConfigurationRepository;

    @Inject
    private UserRepository userRepository;

    @Inject
    private AuthenticationService authenticationService;

    public String createAttachment(Attachment attachment) throws StorageSizeException {
        attachmentRepository.createAttachment(attachment);
        String attachmentId = attachment.getAttachmentId();
        userAttachmentRepository.addAttachmentId(authenticationService.getCurrentUser().getLogin(),
                attachmentId);

        User currentUser = authenticationService.getCurrentUser();
        DomainConfiguration domainConfiguration =
                domainConfigurationRepository.findDomainConfigurationByDomain(currentUser.getDomain());

        long newAttachmentsSize = currentUser.getAttachmentsSize() + attachment.getSize();
        if (newAttachmentsSize > domainConfiguration.getStorageSizeAsLong()) {
            log.info("User " + currentUser.getLogin() +
                    " has tried to exceed his storage capacity. current storage=" +
                    currentUser.getAttachmentsSize() +
                    ", storage capacity=" +
                    domainConfiguration.getStorageSizeAsLong());

            throw new StorageSizeException("User storage exceeded for user " + currentUser.getLogin());
        }

        currentUser.setAttachmentsSize(newAttachmentsSize);
        try {
            userRepository.updateUser(currentUser);
        } catch (ConstraintViolationException cve) {
            log.info("Constraint violated while updating user " + currentUser + " : " + cve);
            throw cve;
        }

        return attachmentId;
    }

    public Attachment getAttachmentById(String attachmentId) {
        return attachmentRepository.findAttachmentById(attachmentId);
    }

    public Collection<String> getAttachmentIdsForCurrentUser(int pagination) {
        Collection<String> attachmentIds =
                userAttachmentRepository.
                        findAttachmentIds(authenticationService.getCurrentUser().getLogin(),
                                pagination);

        return attachmentIds;
    }

    public void deleteAttachment(Attachment attachment) {
        if (log.isDebugEnabled()) {
            log.debug("Removing attachment : " + attachment);
        }
        User currentUser = authenticationService.getCurrentUser();

        for (String attachmentIdTest : userAttachmentRepository.findAttachmentIds(currentUser.getLogin())) {
            if (attachmentIdTest.equals(attachment.getAttachmentId())) {
                userAttachmentRepository.removeAttachmentId(currentUser.getLogin(), attachment.getAttachmentId());
                attachmentRepository.deleteAttachment(attachment);
                long newAttachmentsSize = currentUser.getAttachmentsSize() - attachment.getSize();
                currentUser.setAttachmentsSize(newAttachmentsSize);
                break;
            }
        }
    }
}
