package com.nubo.domain.board.repository;

import com.nubo.domain.board.entity.BoardInvitation;
import com.nubo.domain.board.type.InvitationStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BoardInvitationRepository extends JpaRepository<BoardInvitation, Long> {

  Optional<BoardInvitation> findByIdAndInviteeId(Long invitationId, Long inviteeId);

  Optional<Object> findByBoardIdAndInviteeIdAndStatus(Long id, Long id1,
    InvitationStatus invitationStatus);

  List<BoardInvitation> findAllByBoardId(Long id);
}
