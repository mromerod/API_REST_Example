package com.example.dao;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;//cuidado con este SORT
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.example.entities.Producto;

public interface ProductoDao extends JpaRepository<Producto, Long> {
    //por poner LAZY en jpa repository no me genera las querys porque me trae los datos perezosamente
    //entonces hql

    /**
     * Vamos a necesitar 3 métodos adicionales que genera el crud repository
     * (interface) para :
     * 1.recuperar la lista de productos ordenados
     * 2.recuperar listado de productos paginados, es decir, no trae todos los productos si no de 10 en 10...
     * 3.consulta para recuperar los productos o las presentaciones con sus productos correspondientes 
     *   sin tener que realizar una subconsulta que sería menos eficiente que un join a las entidades
     *   utilizando hql (hiberbnate query language)
     * 
     */
    

     /**
      *    
     * Crearemos unas consultas personalizadas para cuando se busque un productoo,
     * se recupere la presentacion conjuntamente con dicho producto, y tambien para
     * recuperar no todos los productos, sino por pagina, es decir, de 10 en 10, de 20
     * en 20, etc.
     * 
     * RECORDEMOS QUE: Cuando hemos creado las relaciones hemos especificado que 
     * la busqueda sea LAZY, para que no se traiga la presentacion siempre que se 
     * busque un producto, porque serian dos consultas, o una consulta con una 
     * subconsulta, que es menos eficiente que lo que vamos a hacer, hacer una sola 
     * consulta relacionando las entidades, y digo las entidades, porque aunque 
     * de la impresión que es una consulta de SQL no consultamos a las tablas de 
     * la base de datos sino a las entidades 
     * (esto se llama HQL (Hibernate Query Language))
     * 
     * Ademas, tambien podremos recuperar el listado de productos de forma ordenada, 
     * por algun criterio de ordenación, como por ejemplo el nombre del producto o
     * descripcion
      */

     //REcupera lista de productos ordenados
    @Query(value = "select from Producto p left join fetch p.presentacion") //consultas a las entidades hql
     public List<Producto> findAll(Sort sort);


    //recupera una pagina de producto
    @Query(value = "select from Producto p left join fetch p.presentacion",
     countQuery = "select count(p) from Producto p left join left join p.presentacion")
     public Page<Producto> findAll(Pageable pageable);

    //El metodo siguiente recupera el producto por ID, para que nos traiga presentacion tambien

    @Query(value = "select from Producto p left join fetch p.presentacion where p.id= :id")
    
    public List<Producto> findById(long id);


}
