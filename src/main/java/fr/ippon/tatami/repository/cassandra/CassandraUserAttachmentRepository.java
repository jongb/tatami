package fr.ippon.tatami.repository.cassandra;

import fr.ippon.tatami.config.Constants;
import fr.ippon.tatami.repository.UserAttachmentRepository;
import me.prettyprint.cassandra.serializers.LongSerializer;
import me.prettyprint.cassandra.serializers.StringSerializer;
import me.prettyprint.cassandra.service.template.ColumnFamilyResult;
import me.prettyprint.cassandra.service.template.ColumnFamilyTemplate;
import me.prettyprint.cassandra.service.template.ThriftColumnFamilyTemplate;
import me.prettyprint.hector.api.Keyspace;
import me.prettyprint.hector.api.factory.HFactory;
import me.prettyprint.hector.api.mutation.Mutator;
import org.springframework.stereotype.Repository;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;

import static fr.ippon.tatami.config.ColumnFamilyKeys.USER_ATTACHMENT_CF;

/**
 * Cassandra implementation of the UserAttachment repository.
 * <p/>
 * Structure :
 * - Key = login
 * - Name = attachmentId
 * - Value = time
 *
 * @author Julien Dubois
 */
@Repository
public class CassandraUserAttachmentRepository
        implements UserAttachmentRepository {

    private ColumnFamilyTemplate<String, String> attachmentsTemplate;

    @Inject
    private Keyspace keyspaceOperator;

    @PostConstruct
    public void init() {
        attachmentsTemplate = new ThriftColumnFamilyTemplate<String, String>(keyspaceOperator,
                USER_ATTACHMENT_CF,
                StringSerializer.get(),
                StringSerializer.get());
    }

    @Override
    public void addAttachmentId(String login, String attachmentId) {
        Mutator<String> mutator = HFactory.createMutator(keyspaceOperator, StringSerializer.get());
        mutator.insert(login, USER_ATTACHMENT_CF, HFactory.createColumn(attachmentId,
                Calendar.getInstance().getTimeInMillis(), StringSerializer.get(), LongSerializer.get()));
    }

    @Override
    public void removeAttachmentId(String login, String attachmentId) {
        Mutator<String> mutator = HFactory.createMutator(keyspaceOperator, StringSerializer.get());
        mutator.delete(login, USER_ATTACHMENT_CF, attachmentId, StringSerializer.get());
    }

    @Override
    public Collection<String> findAttachmentIds(String login, int pagination) {
        ColumnFamilyResult<String, String> result = attachmentsTemplate.queryColumns(login);
        Collection<String> attachmentIds = new ArrayList<String>();
        int index = 0;
        for (String columnName : result.getColumnNames()) {
            if (index > pagination + Constants.PAGINATION_SIZE) {
                break;
            }
            if (index >= pagination) {
                attachmentIds.add(columnName);
            }
            index++;
        }
        return attachmentIds;
    }

    @Override
    public Collection<String> findAttachmentIds(String login) {
        ColumnFamilyResult<String, String> result = attachmentsTemplate.queryColumns(login);
        Collection<String> attachmentIds = new ArrayList<String>();
        for (String columnName : result.getColumnNames()) {
            attachmentIds.add(columnName);
        }
        return attachmentIds;
    }
}
