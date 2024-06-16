package damas.dao;

import modelo.*;
import damas.util.ConexionBD;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

public class PartidasDAOImpl implements PartidasDAO {

	@Override
	public int crearPartidaBD(int idUsuarioDesafiado, int idUsuarioDesafiador, int tamanio) {
		// esto devolverá el id de la partida
		int idPartidaCreada = -1;
		String insertarPartidasSQL     = "INSERT INTO partidas (tamanio, finalizada) VALUES (?, ?)";
		String insertUsuarioPartidaSQL = "INSERT INTO usuarios_partidas (id_usuario, id_partida, color) VALUES (?, ?, ?)";
		String obtenerUltimoIdPartida  = "SELECT id_partida FROM partidas order by id_partida DESC limit 1;";

		try (Connection connection = ConexionBD.obtenerConexion()){
			// Insertar la nueva partida
			try (PreparedStatement preparedStatement = connection.prepareStatement(insertarPartidasSQL)) {
				preparedStatement.setInt(1, tamanio);
				preparedStatement.setInt(2, 0);
				preparedStatement.executeUpdate();
			}

			// Obtener el ID de la última partida insertada
			try (PreparedStatement preparedStatement = connection.prepareStatement(obtenerUltimoIdPartida);
				 ResultSet resultSet = preparedStatement.executeQuery()) {
				if (resultSet.next()) {
					idPartidaCreada = resultSet.getInt("id_partida");
				}
			}

			// Insertar al usuario desafiado como Blanco
			// Este usuario tiene el primer turno
			try (PreparedStatement preparedStatement = connection.prepareStatement(insertUsuarioPartidaSQL)) {
				preparedStatement.setInt(1, idUsuarioDesafiado);
				preparedStatement.setInt(2, idPartidaCreada);
				preparedStatement.setString(3, "B");
				preparedStatement.executeUpdate();
			}

			// Insertar al usuario desafiador como Negro
			try (PreparedStatement preparedStatement = connection.prepareStatement(insertUsuarioPartidaSQL)) {
				preparedStatement.setInt(1, idUsuarioDesafiador);
				preparedStatement.setInt(2, idPartidaCreada);
				preparedStatement.setString(3, "N");
				preparedStatement.executeUpdate();
			}

		} catch (SQLException e) {
			e.printStackTrace();
		}

		return idPartidaCreada;
	}

	@Override
	public void insertarMovimientoBD(int idPartida, int idUsuario, int posXini, int posYini, int posXfin, int posYfin) {

		String sql = "INSERT INTO movimientos (id_usuario, id_partida, pos_ini_x, pos_ini_y, pos_fin_x, pos_fin_y) VALUES (?, ?, ?, ?, ?, ?)";

		try (Connection connection = ConexionBD.obtenerConexion();
			 PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
			preparedStatement.setInt(1, idUsuario);
			preparedStatement.setInt(2, idPartida);
			preparedStatement.setInt(3, posXini);
			preparedStatement.setInt(4, posYini);
			preparedStatement.setInt(5, posXfin);
			preparedStatement.setInt(6, posYfin);

			int rowsAffected = preparedStatement.executeUpdate();

			if (rowsAffected == 1) {
				// La inserción fue exitosa
				System.out.println("Operación de inserción exitosa.");
				connection.commit();
			} else {
				// La inserción no tuvo efecto (ninguna fila afectada)
				System.out.println("Error en la operación de inserción.");
			}

		} catch (SQLException e) {
			System.err.println("Error: " + e);
		}

	}


	@Override
	public MovimientosPartida devolverPartidaBD(int idPartida) {
		return obtenerMovimientosDeUnaPartida(idPartida);
	}

	@Override
	public void rendirseEnPartidaBD(int idPartida) {
		String sql = "UPDATE partidas SET finalizada = 1 WHERE id_partida = ?";

		try (Connection connection = ConexionBD.obtenerConexion();
			 PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

			preparedStatement.setInt(1, idPartida);
			int rowsUpdated = preparedStatement.executeUpdate();

			if (rowsUpdated == 1) {
				System.out.println("La partida con ID " + idPartida + " ha sido finalizada.");
			} else {
				System.out.println("No se encontró la partida con ID " + idPartida + ".");
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	@Override
	public ArrayList<MovimientosPartida> devolverPartidasActivasBD() {
		return obtenerMovimientosPartidas(false);
	}

	@Override
	public ArrayList<MovimientosPartida> devolverPartidasTerminadasBD() {
		return obtenerMovimientosPartidas(true);
	}

	private ArrayList<MovimientosPartida> obtenerMovimientosPartidas(boolean finalizadas) {
		ArrayList<MovimientosPartida> movimientosTodasPartidas = new ArrayList<>();
		//Sentencia para obtener los ides de las partidas que no estan terminadas
		String sql = "SELECT id_partida FROM partidas WHERE finalizada = ?";

		try (Connection connection = ConexionBD.obtenerConexion();
			 PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

			preparedStatement.setInt(1, finalizadas ? 1 : 0);

			ResultSet resultSet = preparedStatement.executeQuery();

			while (resultSet.next()) {
				int idPartida = resultSet.getInt("id_partida");
				//Recopilar los movimientos de cada partida
				MovimientosPartida movimientosPartida = obtenerMovimientosDeUnaPartida(idPartida);
				movimientosTodasPartidas.add(movimientosPartida);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}

		return movimientosTodasPartidas;
	}


	public MovimientosPartida obtenerMovimientosDeUnaPartida(int idPartida) {

		int tamanioPartida = obtenerTamanioPartida(idPartida);
		int idJugadorBlancas = obtenerIdJugadorBoN(idPartida, "B");
		int idJugadorNegras = obtenerIdJugadorBoN(idPartida, "N");
		int turnoActual = obtenerUltimoTurno(idPartida) == idJugadorBlancas ? idJugadorNegras : idJugadorBlancas;

		ArrayList<Movimiento> movimientos = new ArrayList<>();

		String sql = "SELECT * FROM movimientos WHERE id_partida = ? ORDER BY id_movimiento";
		try (Connection connection = ConexionBD.obtenerConexion();
			 PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
			preparedStatement.setInt(1, idPartida);
			ResultSet rsMovimientos = preparedStatement.executeQuery();
			while (rsMovimientos.next()) {
				Movimiento movimiento = new Movimiento();
				movimiento.setId(rsMovimientos.getInt("id_movimiento"));
				movimiento.setPosIniX(rsMovimientos.getInt("pos_ini_x"));
				movimiento.setPosIniY(rsMovimientos.getInt("pos_ini_y"));
				movimiento.setPosFinX(rsMovimientos.getInt("pos_fin_x"));
				movimiento.setPosFinY(rsMovimientos.getInt("pos_fin_y"));
				movimientos.add(movimiento);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return new MovimientosPartida(idPartida, idJugadorBlancas, idJugadorNegras, turnoActual, tamanioPartida, movimientos);
	}

	private int obtenerTamanioPartida(int idPartida) {
		String sql = "SELECT tamanio FROM partidas WHERE id_partida = ?";
		try (Connection connection = ConexionBD.obtenerConexion();
			 PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

			preparedStatement.setInt(1, idPartida);
			ResultSet rsTamano = preparedStatement.executeQuery();
			if (rsTamano.next()) {
				return rsTamano.getInt("tamanio");
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return -1; // Valor por defecto si no se encuentra el tamaño de la partida
	}

	public int obtenerUltimoTurno(int idPartida) {

		String sql = "SELECT id_usuario FROM movimientos WHERE id_partida = ? order by id_movimiento DESC LIMIT 1";

		try (Connection connection = ConexionBD.obtenerConexion();
			 PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

			preparedStatement.setInt(1, idPartida);

			ResultSet resultSet = preparedStatement.executeQuery();

			if (resultSet.next()) {
				return resultSet.getInt("id_usuario");
			}

		} catch (SQLException e) {
			System.err.println("Error: " + e);

		}
		return -1;
	}

	// Métodos para obtener los ID de los jugadores
	public int obtenerIdJugadorBoN(int idPartida, String color) {
		String sql = "SELECT id_usuario FROM usuarios_partidas WHERE id_partida = ? AND color = ?";
		try (Connection connection = ConexionBD.obtenerConexion();
			 PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
			preparedStatement.setInt(1, idPartida);
			preparedStatement.setString(2, color);
			ResultSet resultSet = preparedStatement.executeQuery();
			if (resultSet.next()) {
				return resultSet.getInt("id_usuario");
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return -1;
	}




}
