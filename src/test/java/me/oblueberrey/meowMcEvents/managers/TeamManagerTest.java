package me.oblueberrey.meowMcEvents.managers;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TeamManagerTest {

    private TeamManager teamManager;

    @Mock
    private Player player1;
    @Mock
    private Player player2;
    @Mock
    private Player player3;
    @Mock
    private Player player4;
    @Mock
    private Player player5;
    @Mock
    private Player player6;

    private UUID uuid1, uuid2, uuid3, uuid4, uuid5, uuid6;

    @BeforeEach
    void setUp() {
        teamManager = new TeamManager();

        // Setup UUIDs
        uuid1 = UUID.randomUUID();
        uuid2 = UUID.randomUUID();
        uuid3 = UUID.randomUUID();
        uuid4 = UUID.randomUUID();
        uuid5 = UUID.randomUUID();
        uuid6 = UUID.randomUUID();

        // Mock player UUIDs
        when(player1.getUniqueId()).thenReturn(uuid1);
        when(player2.getUniqueId()).thenReturn(uuid2);
        when(player3.getUniqueId()).thenReturn(uuid3);
        when(player4.getUniqueId()).thenReturn(uuid4);
        when(player5.getUniqueId()).thenReturn(uuid5);
        when(player6.getUniqueId()).thenReturn(uuid6);
    }

    // ==================== Team Assignment Tests ====================

    @Test
    void assignTeams_ShouldCreateCorrectNumberOfTeams_ForTeamSizeOf2() {
        List<Player> players = Arrays.asList(player1, player2, player3, player4);

        teamManager.assignTeams(players, 2);

        assertEquals(2, teamManager.getTeamCount());
        assertEquals(4, teamManager.getTotalPlayers());
    }

    @Test
    void assignTeams_ShouldCreateCorrectNumberOfTeams_ForTeamSizeOf3() {
        List<Player> players = Arrays.asList(player1, player2, player3, player4, player5, player6);

        teamManager.assignTeams(players, 3);

        assertEquals(2, teamManager.getTeamCount());
        assertEquals(6, teamManager.getTotalPlayers());
    }

    @Test
    void assignTeams_ShouldHandleUnevenPlayerCounts() {
        List<Player> players = Arrays.asList(player1, player2, player3, player4, player5);

        teamManager.assignTeams(players, 2);

        // 5 players with team size 2 = 3 teams (2+2+1)
        assertEquals(3, teamManager.getTeamCount());
        assertEquals(5, teamManager.getTotalPlayers());
    }

    @Test
    void assignTeams_ShouldAssignAllPlayersToTeams() {
        List<Player> players = Arrays.asList(player1, player2, player3, player4);

        teamManager.assignTeams(players, 2);

        // Each player should have a team
        assertTrue(teamManager.getTeam(player1) > 0);
        assertTrue(teamManager.getTeam(player2) > 0);
        assertTrue(teamManager.getTeam(player3) > 0);
        assertTrue(teamManager.getTeam(player4) > 0);
    }

    @Test
    void assignTeams_ShouldHandleNullPlayers() {
        teamManager.assignTeams(null, 2);
        assertEquals(0, teamManager.getTeamCount());
    }

    @Test
    void assignTeams_ShouldHandleEmptyList() {
        teamManager.assignTeams(new ArrayList<>(), 2);
        assertEquals(0, teamManager.getTeamCount());
    }

    @Test
    void assignTeams_ShouldHandleInvalidTeamSize() {
        List<Player> players = Arrays.asList(player1, player2);
        teamManager.assignTeams(players, 0);
        assertEquals(0, teamManager.getTeamCount());
    }

    @Test
    void assignTeams_ShouldClearPreviousTeams() {
        List<Player> players1 = Arrays.asList(player1, player2);
        teamManager.assignTeams(players1, 1);
        assertEquals(2, teamManager.getTeamCount());

        List<Player> players2 = Arrays.asList(player3, player4, player5, player6);
        teamManager.assignTeams(players2, 2);
        assertEquals(2, teamManager.getTeamCount());

        // Old players should no longer be in teams
        assertEquals(-1, teamManager.getTeam(player1));
        assertEquals(-1, teamManager.getTeam(player2));
    }

    // ==================== Team Membership Tests ====================

    @Test
    void getTeam_ShouldReturnCorrectTeamNumber() {
        List<Player> players = Arrays.asList(player1, player2, player3, player4);
        teamManager.assignTeams(players, 2);

        int team1 = teamManager.getTeam(player1);
        int team2 = teamManager.getTeam(player2);

        assertTrue(team1 >= 1 && team1 <= 2);
        assertTrue(team2 >= 1 && team2 <= 2);
    }

    @Test
    void getTeam_ShouldReturnNegativeOneForUnassignedPlayer() {
        assertEquals(-1, teamManager.getTeam(player1));
    }

    @Test
    void getTeam_ShouldReturnNegativeOneForNullPlayer() {
        assertEquals(-1, teamManager.getTeam((Player) null));
    }

    @Test
    void getTeam_ByUUID_ShouldReturnCorrectTeamNumber() {
        List<Player> players = Arrays.asList(player1, player2);
        teamManager.assignTeams(players, 1);

        int team = teamManager.getTeam(uuid1);
        assertTrue(team >= 1);
    }

    @Test
    void isSameTeam_ShouldReturnTrueForTeammates() {
        List<Player> players = Arrays.asList(player1, player2);
        teamManager.assignTeams(players, 2);

        // Both players are on the same team (size 2)
        assertTrue(teamManager.isSameTeam(player1, player2));
    }

    @Test
    void isSameTeam_ShouldReturnFalseForDifferentTeams() {
        List<Player> players = Arrays.asList(player1, player2, player3, player4);
        teamManager.assignTeams(players, 2);

        // Find two players on different teams
        int team1 = teamManager.getTeam(player1);
        Player differentTeamPlayer = null;

        for (Player p : Arrays.asList(player2, player3, player4)) {
            if (teamManager.getTeam(p) != team1) {
                differentTeamPlayer = p;
                break;
            }
        }

        if (differentTeamPlayer != null) {
            assertFalse(teamManager.isSameTeam(player1, differentTeamPlayer));
        }
    }

    @Test
    void isSameTeam_ShouldReturnFalseForNullPlayers() {
        assertFalse(teamManager.isSameTeam(null, player1));
        assertFalse(teamManager.isSameTeam(player1, null));
        assertFalse(teamManager.isSameTeam(null, null));
    }

    // ==================== Team Removal Tests ====================

    @Test
    void removeFromTeam_ShouldRemovePlayerFromTeam() {
        List<Player> players = Arrays.asList(player1, player2);
        teamManager.assignTeams(players, 2);

        int initialTeam = teamManager.getTeam(player1);
        assertTrue(initialTeam > 0);

        teamManager.removeFromTeam(player1);

        assertEquals(-1, teamManager.getTeam(player1));
    }

    @Test
    void removeFromTeam_ShouldRemoveEmptyTeams() {
        List<Player> players = Arrays.asList(player1, player2);
        teamManager.assignTeams(players, 1); // Solo mode - each player is own team

        assertEquals(2, teamManager.getTeamCount());

        teamManager.removeFromTeam(player1);

        assertEquals(1, teamManager.getTeamCount());
    }

    @Test
    void removeFromTeam_ShouldHandleNullPlayer() {
        // Should not throw exception
        teamManager.removeFromTeam((Player) null);
        teamManager.removeFromTeam((UUID) null);
    }

    // ==================== Winner Detection Tests ====================

    @Test
    void isTeamAlive_ShouldReturnTrueWhenTeamHasAlivePlayers() {
        List<Player> players = Arrays.asList(player1, player2);
        teamManager.assignTeams(players, 2);

        Set<UUID> alivePlayers = new HashSet<>(Arrays.asList(uuid1, uuid2));
        int teamNumber = teamManager.getTeam(player1);

        assertTrue(teamManager.isTeamAlive(teamNumber, alivePlayers));
    }

    @Test
    void isTeamAlive_ShouldReturnFalseWhenNoTeamMembersAlive() {
        List<Player> players = Arrays.asList(player1, player2);
        teamManager.assignTeams(players, 2);

        Set<UUID> alivePlayers = new HashSet<>(); // No one alive
        int teamNumber = teamManager.getTeam(player1);

        assertFalse(teamManager.isTeamAlive(teamNumber, alivePlayers));
    }

    @Test
    void isTeamAlive_ShouldReturnFalseForNullAlivePlayers() {
        List<Player> players = Arrays.asList(player1, player2);
        teamManager.assignTeams(players, 2);

        assertFalse(teamManager.isTeamAlive(1, null));
    }

    @Test
    void getAliveTeamCount_ShouldReturnCorrectCount() {
        List<Player> players = Arrays.asList(player1, player2, player3, player4);
        teamManager.assignTeams(players, 2);

        // All players alive
        Set<UUID> allAlive = new HashSet<>(Arrays.asList(uuid1, uuid2, uuid3, uuid4));
        assertEquals(2, teamManager.getAliveTeamCount(allAlive));

        // Only one team alive (assuming team 1 has player1 and player2)
        int team1 = teamManager.getTeam(player1);
        Set<UUID> team1Members = teamManager.getTeamMembers(team1);
        assertEquals(1, teamManager.getAliveTeamCount(team1Members));
    }

    @Test
    void getAliveTeamCount_ShouldReturnZeroForNullInput() {
        assertEquals(0, teamManager.getAliveTeamCount(null));
    }

    @Test
    void getWinningTeam_ShouldReturnLastAliveTeam() {
        List<Player> players = Arrays.asList(player1, player2, player3, player4);
        teamManager.assignTeams(players, 2);

        // Get team 1's members
        int team1 = teamManager.getTeam(player1);
        Set<UUID> team1Members = teamManager.getTeamMembers(team1);

        // Only team 1 is alive
        int winningTeam = teamManager.getWinningTeam(team1Members);
        assertEquals(team1, winningTeam);
    }

    @Test
    void getWinningTeam_ShouldReturnNegativeOneWhenNoTeamsAlive() {
        List<Player> players = Arrays.asList(player1, player2);
        teamManager.assignTeams(players, 2);

        Set<UUID> noOneAlive = new HashSet<>();
        assertEquals(-1, teamManager.getWinningTeam(noOneAlive));
    }

    @Test
    void getWinningTeam_ShouldReturnNegativeOneForNullInput() {
        assertEquals(-1, teamManager.getWinningTeam(null));
    }

    // ==================== Team Mode Tests ====================

    @Test
    void isTeamMode_ShouldReturnFalseWhenNoTeamsAssigned() {
        assertFalse(teamManager.isTeamMode());
    }

    @Test
    void isTeamMode_ShouldReturnTrueWhenTeamsAssigned() {
        List<Player> players = Arrays.asList(player1, player2);
        teamManager.assignTeams(players, 2);

        assertTrue(teamManager.isTeamMode());
    }

    @Test
    void clearTeams_ShouldRemoveAllTeams() {
        List<Player> players = Arrays.asList(player1, player2, player3, player4);
        teamManager.assignTeams(players, 2);

        assertEquals(2, teamManager.getTeamCount());

        teamManager.clearTeams();

        assertEquals(0, teamManager.getTeamCount());
        assertEquals(0, teamManager.getTotalPlayers());
        assertFalse(teamManager.isTeamMode());
    }

    // ==================== Team Color Tests ====================

    @Test
    void getTeamColor_ShouldReturnCorrectColorForValidTeam() {
        assertEquals(ChatColor.RED, teamManager.getTeamColor(1));
        assertEquals(ChatColor.BLUE, teamManager.getTeamColor(2));
        assertEquals(ChatColor.GREEN, teamManager.getTeamColor(3));
        assertEquals(ChatColor.YELLOW, teamManager.getTeamColor(4));
    }

    @Test
    void getTeamColor_ShouldReturnGrayForInvalidTeam() {
        assertEquals(ChatColor.GRAY, teamManager.getTeamColor(0));
        assertEquals(ChatColor.GRAY, teamManager.getTeamColor(9));
        assertEquals(ChatColor.GRAY, teamManager.getTeamColor(-1));
    }

    @Test
    void getFormattedTeamName_ShouldReturnColoredName() {
        String teamName = teamManager.getFormattedTeamName(1);
        assertTrue(teamName.contains("Team 1"));
        assertTrue(teamName.contains(ChatColor.RED.toString()));
    }

    // ==================== Utility Tests ====================

    @Test
    void getAllTeamNumbers_ShouldReturnAllTeamNumbers() {
        List<Player> players = Arrays.asList(player1, player2, player3, player4);
        teamManager.assignTeams(players, 2);

        Set<Integer> teamNumbers = teamManager.getAllTeamNumbers();
        assertEquals(2, teamNumbers.size());
        assertTrue(teamNumbers.contains(1));
        assertTrue(teamNumbers.contains(2));
    }

    @Test
    void getTeamMembers_ShouldReturnEmptySetForInvalidTeam() {
        Set<UUID> members = teamManager.getTeamMembers(999);
        assertNotNull(members);
        assertTrue(members.isEmpty());
    }
}
