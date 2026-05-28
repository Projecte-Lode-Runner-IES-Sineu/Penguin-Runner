/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package iessineu.penguinrunner;

/**
 *
 * @author loren
 */
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import iessineu.penguinrunner.Blocks.Block;
import iessineu.penguinrunner.Blocks.Door;
import iessineu.penguinrunner.Blocks.Ice;
import iessineu.penguinrunner.Blocks.IceCream;
import iessineu.penguinrunner.Blocks.Ladder;
import iessineu.penguinrunner.Blocks.Molten;
import iessineu.penguinrunner.Blocks.Rail;
import iessineu.penguinrunner.Blocks.Stone;
import iessineu.penguinrunner.Blocks.TileType;
import iessineu.penguinrunner.Blocks.Wall;
import iessineu.penguinrunner.Entity.Enemy;
import iessineu.penguinrunner.Entity.Map;
import iessineu.penguinrunner.Entity.Player;
import iessineu.penguinrunner.Movement.Direction;
import iessineu.penguinrunner.States.ClimbingState;
import iessineu.penguinrunner.States.FallingState;
import iessineu.penguinrunner.States.PlayerState;
import iessineu.penguinrunner.States.RailState;
import iessineu.penguinrunner.States.WalkingState;

public class GameState implements Serializable {

    private final List<BrokenBlock> brokenBlocks = new ArrayList<>();
    private List<Stone> stoneBlocks = new ArrayList<>();
    private Player player;
    private List<Enemy> enemies;
    private int iceCream = 0;
    private Map mapObject;

    private final int startPlayerRow;
    private final int startPlayerCol;

    private final PlayerState walkingState = new WalkingState();
    private final PlayerState climbingState = new ClimbingState();
    private final PlayerState railState = new RailState();
    private final PlayerState fallingState = new FallingState();

    public GameState() {
        List<Map> mapList = llegirMapes();
        this.mapObject = mapList.get(0);

        String[] level = mapObject.getMap();

        blocks = new Block[level.length][level[0].length()];
        enemies = new ArrayList<>();

        Player foundPlayer = null;

        int tempStartRow = 1;
        int tempStartCol = 1;

        for (int row = 0; row < level.length; row++) {
            for (int col = 0; col < level[row].length(); col++) {
                char symbol = level[row].charAt(col);

                switch (symbol) {
                    case '#':
                        blocks[row][col] = new Wall(row, col);
                        break;

                    case '.':
                        blocks[row][col] = new Ice(row, col);
                        break;

                    case 'G':
                        blocks[row][col] = new IceCream(row, col);
                        iceCream++;
                        break;

                    case 'H':
                        blocks[row][col] = new Ladder(row, col);
                        break;

                    case '-':
                        blocks[row][col] = new Rail(row, col);
                        break;

                    case 'D':
                        blocks[row][col] = new Door(row, col);
                        break;

                    case 'S':
                        Stone stone = new Stone(row, col);
                        blocks[row][col] = stone;
                        stones.add(stone);
                        break;

                    case 'P':
                        foundPlayer = new Player(row, col);
                        tempStartRow = row;
                        tempStartCol = col;
                        blocks[row][col] = null;
                        break;

                    case 'E':
                        enemies.add(new Enemy(row, col, 1, 1));
                        blocks[row][col] = null;
                        break;

                    default:
                        blocks[row][col] = null;
                        break;
                }
            }
        }

        player = foundPlayer;

        startPlayerRow = tempStartRow;
        startPlayerCol = tempStartCol;

        updatePlayerState();
    }

    /*
     * Guardem les posicions inicials per poder reiniciar
     * quan l'enemic atrapa el jugador.
     */
    private int startPlayerRow;
    private int startPlayerCol;

    /*
     * Cada vegada que el jugador prem una fletxa:
     * 1. Es mou el jugador.
     * 2. Es mouen els enemics.
     * 3. Es comproven col·lisions.
     */
    public void takeTurn(Direction direction) {
        updatePlayerState();

        if (direction != null) {
            movePlayer(direction);
        }
        moveBlocks();
        collectIcecream();
        moveEnemies();
        updateBrokenBlocks();
        checkCollisions();
        spriteJSON("type");
        updatePlayerState();
    }

    /*
     * Mou el jugador una casella en la direcció indicada.
     */

    private void updatePlayerState() {
        if (shouldPlayerDrop()) {
            player.setState(fallingState);
        } else if (isStair(player.getRow(), player.getCol())) {
            player.setState(climbingState);
        } else if (isRail(player.getRow(), player.getCol())) {
            player.setState(railState);
        } else {
            player.setState(walkingState);
        }

        if (isStone(nextRow, nextCol)) {
            boolean stoneMoved = tryPushStone(actualRow, actualCol, direction);

            if (!stoneMoved) {
                return;
            }
        }
        // Per a la resta de direccions, aplica les regles normals
        if (canMoveTo(nextRow, nextCol)) {
            player.setPosition(nextRow, nextCol);
        }

    }

    private boolean tryPushStone(int row, int playerCol, Direction direction) {
        int dc = direction.getDc();

        if (dc == 0) {
            return false;
        }

        int firstStoneCol = playerCol + dc;
        int checkCol = firstStoneCol;

        while (!isOutOfBounds(row, checkCol) && isStone(row, checkCol)) {
            checkCol += dc;
        }

        if (isOutOfBounds(row, checkCol) || !isBlank(row, checkCol)) {
            return false;
        }

        int col = checkCol - dc;

        while (col != playerCol) {
            map[row][col + dc] = TileType.STONE;
            map[row][col] = TileType.BLANK;

            updateStoneObject(row, col, col + dc);

            col -= dc;
        }

        return true;
    }

    private void updateStoneObject(int row, int oldCol, int newCol) {
        for (Stone stone : stoneBlocks) {
            if (stone.getRow() == row && stone.getCol() == oldCol) {
                stone.setPosition(row, newCol);
                return;
            }
        }
    }

    public void interact() {
        if (map[player.getRow()][player.getCol()] == TileType.DOOR) {
            System.out.println("Porta");
            int nivellActual = mapObject.getLevel();
            mapObject = mapList.get(nivellActual);
            map = stringToTileType();
        }
    }

    public TileType[][] stringToTileType() {
        /*
         * Mapa del nivell.
         *
         * # = paret
         * . = terra
         * G = or
         * P = jugador
         * E = enemic
         * D = porta
         */
        String[] level = mapObject.getMap();
        map = new TileType[level.length][level[0].length()];
        iceCream = 0;
        enemies = new ArrayList();

        Player foundPlayer = null;

        /*
         * Valors temporals per recordar on comença el jugador.
         */
        int tempStartRow = 1;
        int tempStartCol = 1;

        /*
         * Convertim el mapa de text a TileType[][].
         */
        for (int row = 0; row < level.length; row++) {
            for (int col = 0; col < level[row].length(); col++) {

                char symbol = level[row].charAt(col);

                switch (symbol) {
                    case '#' ->
                        map[row][col] = TileType.WALL;
                    case 'G' -> {
                        map[row][col] = TileType.ICECREAM;
                        iceCream++;
                    }
                    case '.' ->
                        map[row][col] = TileType.ICE;
                    case 'H' ->
                        map[row][col] = TileType.STAIR;
                    case '-' ->
                        map[row][col] = TileType.RAIL;
                    case 'D' ->
                        map[row][col] = TileType.DOOR;
                    case 'P' -> {
                        // Si trobem el jugador, el creem.
                        // La casella on hi havia P passa a ser terra.
                        foundPlayer = new Player(row, col);
                        tempStartRow = row;
                        tempStartCol = col;
                    }
                    case 'E' ->
                        // Si trobem un enemic, l'afegim a la llista.
                        // La casella on hi havia E passa a ser terra.
                        enemies.add(new Enemy(row, col, 1, 1));
                    case 'S' -> {
                        stoneBlocks.add(new Stone(row, col));
                        map[row][col] = TileType.STONE;
                        break;
                    }
                    default ->
                        map[row][col] = TileType.BLANK;
                }
            }
        }

        player = foundPlayer;

        startPlayerRow = tempStartRow;
        startPlayerCol = tempStartCol;
        return map;
    }

    /*
       * Fa accions
     */
    public void breakDownLeft() {
        breakBlock(player.getRow() + 1, player.getCol() - 1);
    }

    public void breakDownRight() {
        breakBlock(player.getRow() + 1, player.getCol() + 1);
    }

    private void breakBlock(int row, int col) {
        if (isOutOfBounds(row, col)) {
            return;
        }
        if (map[row][col] == TileType.ICE) {
            map[row][col] = TileType.MOLTEN;
            brokenBlocks.add(new BrokenBlock(row, col, 5));
        }

    }

    /*
     * Mou tots els enemics.
     */
    private void moveEnemies() {
        for (Enemy enemy : enemies) {

            if (enemy.getIsDead()) {
                enemy.subtractTimeToRevive(1);
                if (enemy.getTimeToRevive() <= 0) {
                    enemy.revive();
                }
            } else {
                if (!isFos(enemy.getRow(), enemy.getCol())) {
                    moveEnemy(enemy);
                }

            }

        }
    }

    /*
    * Comprova si la pedra ha de caure
     */
    private void moveBlocks() {
        for (Stone s : stoneBlocks) {
            int row = s.getRow();
            int col = s.getCol();

            if (!isOutOfBounds(row + 1, col) && map[row + 1][col] == TileType.BLANK) {
                map[row][col] = TileType.BLANK;
                map[row + 1][col] = TileType.STONE;

                s.setPosition(row + 1, col);
            }
        }
    }

    /*
     * Mou un enemic cap al jugador de manera molt senzilla.
     *
     * Primer intenta acostar-se en vertical.
     * Si ja està a la mateixa fila, intenta acostar-se en horitzontal.
     */
    private void moveEnemy(Enemy enemy) {
        int row = enemy.getRow();
        int col = enemy.getCol();
        int dr = 0;
        int dc = 0;
        if (shouldDie(row, col) && !enemy.getIsDead()) {
            enemy.die();
            enemy.setTimeToRevive(7);
            return;
        }
        if (shouldDrop(row, col)) {
            enemy.setPosition(row + 1, col);
            return;
        }
        if (enemy.getRow() < player.getRow()) {
            dr = 1;
        } else if (enemy.getRow() > player.getRow()) {
            //amunt, de moment desactivat, falta fer comprovacio de si esta a una escala
        } else if (enemy.getCol() < player.getCol()) {
            dc = 1;
        } else if (enemy.getCol() > player.getCol()) {
            dc = -1;
        }

        int nextRow = enemy.getRow() + dr;
        int nextCol = enemy.getCol() + dc;

        if (canMoveTo(nextRow, nextCol)) {
            enemy.setPosition(nextRow, nextCol);
        }
    }

    /*
     * Comprova si una posició és vàlida per moure's.
     */
    private boolean canMoveTo(int row, int col) {
        return !isOutOfBounds(row, col)
                && !isWall(row, col)
                && !isIce(row, col)
                && !isStone(row, col);
    }

    /*
     * Comprova si una posició surt fora del mapa.
     */
    private boolean isOutOfBounds(int row, int col) {
        return row < 0
                || row >= getRows()
                || col < 0
                || col >= getCols();
    }

    /*
     * Comprova si una casella és paret.
     */
    private boolean isWall(int row, int col) {
        return map[row][col] == TileType.WALL;
    }

    private boolean isIce(int row, int col) {
        return map[row][col] == TileType.ICE;
    }

    private boolean isRail(int row, int col) {
        return map[row][col] == TileType.RAIL;
    }

    private boolean isStair(int row, int col) {
        return map[row][col] == TileType.STAIR;
    }

    private boolean isFos(int row, int col) {
        return map[row][col] == TileType.MOLTEN;
    }

    private boolean isBlank(int row, int col) {
        return map[row][col] == TileType.BLANK;
    }

    private boolean isStone(int row, int col) {
        return map[row][col] == TileType.STONE;
    }

    private boolean isEnemy(int row, int col) {
        for (Enemy enemy : enemies) {
            if (enemy.getRow() == row && enemy.getCol() == col) {
                return true;
            }

        }
        return false;
    }

    /*
     * Si el jugador trepitja una casella amb gelat,
     * la convertim en res.
     */
    private void collectIcecream() {
        int row = player.getRow();
        int col = player.getCol();

    public TileType getTile(int row, int col) {
        return map[row][col];
    }

    public int getRows() {
        return map.length;
    }

    public int getCols() {
        return map[0].length;
    }

    public Player getPlayer() {
        return player;
    }

    public List<Enemy> getEnemies() {
        return enemies;
    }

    public int getLevel() {
        return mapObject.getLevel();
    }

    public int getIceCream() {
        return iceCream;
    }

    public List<Stone> getStoneBlocks() {
        return stoneBlocks;
    }

    public void setStoneBlocks(List<Stone> stoneBlocks) {
        this.stoneBlocks = stoneBlocks;
    }

    public List<Map> getMapList() {
        return mapList;
    }

    public void setMapList(List<Map> mapList) {
        this.mapList = mapList;
    }

    private static class BrokenBlock implements Serializable {

        int row;
        int col;
        int turnsLeft;

        BrokenBlock(int row, int col, int turnsLeft) {
            this.row = row;
            this.col = col;
            this.turnsLeft = turnsLeft;
        }
    }

    /*
     * LECTURA MAPES
     */

    public String llegirJSON(String ruta) {
        String jsonString = "";
        try {
            BufferedReader fitxer = new BufferedReader(new FileReader(ruta));
            try {
                String c;
                while ((c = fitxer.readLine()) != null) { //llegim linea, si no es null, imprimim la linea sencera
                    jsonString += c;
                }
                fitxer.close();
            } catch (IOException ex) {
                System.out.println("Problema d'entrada i sortida");
            }
        } catch (FileNotFoundException ex) {
            System.out.println("L'arxiu de mapes no s'ha trobat!");
        }
        return jsonString;
    }

    public List<Map> llegirMapes() {
        JSONArray maps = new JSONArray(llegirJSON("resources/maps.json"));
        List<Map> mapList = new ArrayList();
        for (int i = 0; i < maps.length(); i++) {
            JSONObject obj = maps.getJSONObject(i);
            JSONArray jAr = obj.getJSONArray("view");
            String[] view = new String[jAr.length()];
            for (int j = 0; j < view.length; j++) {
                view[j] = jAr.getString(j);
            }
            mapList.add(new Map(obj.getInt("level"), view));
        }
        return mapList;
    }

    public String spriteJSON(String type) {
        JSONArray entities = new JSONArray(llegirJSON("resources/entities.json"));
        String sprite = "";
        for (int i = 0; i < entities.length(); i++) {
            JSONObject obj = entities.getJSONObject(i);
            String objType = obj.getString("type");
            switch (objType) {
                case "tiles" -> {
                    JSONArray tiles = obj.getJSONArray("tiles");
                    for (int j = 0; j < tiles.length(); j++) {
                        obj = (JSONObject) tiles.get(j);
                        System.out.println("Type: " + obj.getString("type"));
                        System.out.println("Sprite: " + obj.getString("sprite"));
                        System.out.println("Color: " + obj.getString("color"));
                    }
                }
                case "player", "enemy" -> {
                    System.out.println("Sprite: " + obj.getString("sprite"));
                    System.out.println("Nom: " + obj.getString("name"));
                    System.out.println("Color: " + obj.getString("color"));
                }
            }
        }
        return "";
    }
}
