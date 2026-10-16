package com.equestrian.equis.repository.notices;

import com.equestrian.equis.entity.notices.Notice;
import com.equestrian.equis.entity.notices.NoticeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface NoticeRepository extends JpaRepository<Notice, UUID> {
    boolean existsByTrainerIdAndTypeAndRefIdAndReadAtIsNull(UUID trainerId, NoticeType type, UUID refId);

    // The newest notice of the type per ref, read or not, to any trainer; refs without any are absent.
    // Don't call with an empty list.
    @Query("""
            select new com.equestrian.equis.repository.notices.RefTime(n.refId, max(n.createdAt))
            from Notice n
            where n.type = :type and n.refId in :refIds
            group by n.refId
            """)
    List<RefTime> findLatestByRefIds(@Param("refIds") Collection<UUID> refIds, @Param("type") NoticeType type);
}
