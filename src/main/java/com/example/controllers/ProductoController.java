package com.example.controllers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.entities.Producto;
import com.example.services.ProductoService;
import com.example.utilities.FileUploadUtil;

import jakarta.validation.Valid;

@RestController //con esto devuelve un json 
@RequestMapping("/productos")


public class ProductoController {

/**El metodo siguiente es de ejemplo para entender el JSON, no es del proyecto*/
/** @GetMapping
    public List<String> nombres() {

        List<String> nombres = Arrays.asList(
            "Salma", "Judith", "Elisabet"
        );

           return nombres;


    } */

    @Autowired
    private ProductoService productoService;

    @Autowired
    private FileUploadUtil fileUploadUtil;

    //El metodo siguiente va a responder a una peticion request del tipo 
    //http://localhost8080/productos?page=3&size=4 (3 paginas con 4 productos cada una)
    //tiene que devolver un listado de productos paginados o no pero en cualquier caso ordenados por un
    //criterio(nombre,descripcion..)
/*
 * /productos/3 -> @PathVariable
 * 
 * para la nuestra @requestparam
 * 
 */
    
@GetMapping
public ResponseEntity<List<Producto>> findAll(@RequestParam(name = "page", required = false) Integer page,
                                                @RequestParam(name = "size", required = false)  Integer size){
 
 
ResponseEntity<List<Producto>> responseEntity = null;
List<Producto> productos = new ArrayList<>();
Sort sortByName = Sort.by("nombre");
if(page !=null && size !=null){
try {

    Pageable pageable = PageRequest.of(page, size, sortByName);
    Page<Producto> productosPaginados = productoService.findAll(pageable);
    productos = productosPaginados.getContent();

    responseEntity = new ResponseEntity<List<Producto>>(productos, HttpStatus.OK);

    


    
}

catch (Exception e) {
    responseEntity = new ResponseEntity<>(HttpStatus.BAD_REQUEST);
}

}else {

    //sin paginacion pero con ordenamiento

try {
    
    productos = productoService.findAll(sortByName);
    responseEntity = new ResponseEntity<List<Producto>>(productos, HttpStatus.OK);
} catch (Exception e) {

    responseEntity = new ResponseEntity<>(HttpStatus.NO_CONTENT);
}

}

return responseEntity;

/**pedir por el id*/

    }
@GetMapping("/{id}")
public ResponseEntity<Map<String, Object>> findById(@PathVariable(name = "id") Integer id){
    
    ResponseEntity<Map<String, Object>> responseEntity = null;
    Map<String, Object> responseAsMap = new HashMap<>();
    Map<String, Object> responseError = new HashMap<>();    
    try {
        Producto producto = productoService.findById(id);

        if(producto != null) {
            String successMessage = "Se ha encontrado el producto con id: " + id;
            responseAsMap.put("mensaje", successMessage);
            responseAsMap.put("producto", producto);
            responseEntity = new ResponseEntity<Map<String, Object>>(responseAsMap, HttpStatus.OK);
        } else{
            String errorMessage = "No se ha encontrado el producto con el id: " + id;
            responseError.put("error", errorMessage);
            responseEntity = new ResponseEntity<Map<String,Object>>(responseError, HttpStatus.NOT_FOUND);
        }

    } catch (Exception e) {

        String errorGrave = "Error grave";
        responseAsMap.put("error", errorGrave);
        responseEntity = new ResponseEntity<Map<String, Object>>(responseAsMap, HttpStatus.INTERNAL_SERVER_ERROR);

    }


return responseEntity;

}
    
/**
 *Guardar  un producto nuevo en la base de datos
 * @throws IOException
 */
@PostMapping(consumes = "multipart/form-data")
@Transactional
public ResponseEntity<Map<String, Object>> insert(
    @Valid @RequestPart(name = "producto") Producto producto, BindingResult result,
    @RequestPart(name = "file") MultipartFile file) throws IOException {

Map<String, Object> responseAsMap = new HashMap<>();
ResponseEntity <Map<String,Object>> responseEntity = null;

/**
 * Primero: Comprobar si hay errores en el producto recibido
 */


 if(result.hasErrors()){

    List<String> errorMessage = new ArrayList<>();


    for(ObjectError error : result.getAllErrors()){

        errorMessage.add(error.getDefaultMessage());

    }

    responseAsMap.put("errores", errorMessage);

    responseEntity = new ResponseEntity<Map<String,Object>>(responseAsMap, HttpStatus.BAD_REQUEST);

    return responseEntity;

 }

 //Si no hay errores ,se guarda si previamente nos han enviado una imagen

 if(!file.isEmpty()){
   
  String fileCode = fileUploadUtil.saveFile(file.getOriginalFilename(), file);
  producto.setImagenProducto(fileCode+ "-" + file.getOriginalFilename());

 }
 Producto productoDB = productoService.save(producto);
 try {
    if(productoDB != null){

        String mensaje = "El producto se ha guardado exitosamente";
        responseAsMap.put("mensaje", mensaje);
        responseAsMap.put("producto", producto);
        responseEntity = new ResponseEntity<Map<String,Object>>(responseAsMap, HttpStatus.CREATED);
    
     }else {
    
        String mensaje = "El producto no se ha creado";
        responseAsMap.put("mensaje", mensaje);
        
        responseEntity = new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
    
     }
    
 } catch (DataAccessException e) { //cuidado porque le excepcion es para no dejarte acceder a los datos 
    String errorGrave = "Ha tenido lugar un error grave" + ", y la causa más probale puede ser"
    + e.getMostSpecificCause();

    responseAsMap.put("errorGrave", errorGrave);

    responseEntity = new ResponseEntity<Map<String,Object>>(responseAsMap, HttpStatus.INTERNAL_SERVER_ERROR);
 }
return responseEntity;

}

/**
 * Actualizar es igual que guardar uno nuevo pero pasando ID
 */

 @PutMapping("/{id}") //porque recibe los datos del formulario
 @Transactional
 public ResponseEntity<Map<String, Object>> update(@Valid @RequestBody Producto producto, BindingResult result,
 @PathVariable(name = "id") Integer id) {
 
 Map<String, Object> responseAsMap = new HashMap<>();
 ResponseEntity <Map<String,Object>> responseEntity = null;
 
 /**
  * Primero: Comprobar si hay errores en el producto recibido
  */
 
 
  if(result.hasErrors()){
 
     List<String> errorMessage = new ArrayList<>();
 
 
     for(ObjectError error : result.getAllErrors()){
 
         errorMessage.add(error.getDefaultMessage());
 
     }
 
     responseAsMap.put("errores", errorMessage);
 
     responseEntity = new ResponseEntity<Map<String,Object>>(responseAsMap, HttpStatus.BAD_REQUEST);
 
     return responseEntity;
 
  }
 
  //Si no hay errores 

  producto.setId(id);
 
  Producto productoDB = productoService.save(producto);
  try {
     if(productoDB != null){
 
         String mensaje = "El producto se ha actualizado exitosamente";
         responseAsMap.put("mensaje", mensaje);
         responseAsMap.put("producto", producto);
         responseEntity = new ResponseEntity<Map<String,Object>>(responseAsMap, HttpStatus.CREATED);
     
      }else {
     
         String mensaje = "El producto no se ha actualizado";
         responseAsMap.put("mensaje", mensaje);
         
         responseEntity = new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
     
      }
     
  } catch (DataAccessException e) { //cuidado porque le excepcion es para no dejarte acceder a los datos 
     String errorGrave = "Ha tenido lugar un error grave" + ", y la causa más probale puede ser"
     + e.getMostSpecificCause();
 
     responseAsMap.put("errorGrave", errorGrave);
 
     responseEntity = new ResponseEntity<Map<String,Object>>(responseAsMap, HttpStatus.INTERNAL_SERVER_ERROR);
  }
 return responseEntity;
 
 }

//Eliminar un producto

@DeleteMapping("/{id}")
@Transactional

 public ResponseEntity<String> delete(@PathVariable(name = "id") Integer id){

ResponseEntity<String> responseEntity = null;



try {
    Producto productoDB = productoService.findById(id);

if(productoDB !=null) {
    productoService.delete(productoDB);

  responseEntity = new ResponseEntity<String>("Borrado exitosamente", HttpStatus.OK);

    }else{

        responseEntity = new ResponseEntity<String>("No existe el producto", HttpStatus.NOT_FOUND);
    }
    
} catch (DataAccessException e) {
    e.getMostSpecificCause();
    responseEntity = new ResponseEntity<String>("error fatal", HttpStatus.INTERNAL_SERVER_ERROR);

}

 

return responseEntity;

 }
}
