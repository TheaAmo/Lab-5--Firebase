package com.example.seg2105lab5

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.seg2105lab5.ui.theme.MyApplicationTheme
import com.google.firebase.database.*

class MainActivity : ComponentActivity() {
    private lateinit var dbRef: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize Firebase Database reference
        dbRef = FirebaseDatabase.getInstance().getReference("products")

        setContent {
            MyApplicationTheme {
                ProductScreen(dbRef = dbRef)
            }
        }
    }
}

// Define the Product data class
data class Product(
    val id: String = "",
    val name: String = "",
    val price: Double = 0.0
)

@Composable
fun ProductScreen(dbRef: DatabaseReference) {
    var productName by remember { mutableStateOf("") }
    var productPrice by remember { mutableStateOf("") }
    val context = LocalContext.current
    val products = remember { mutableStateListOf<Product>() }

    // Read data from Firebase and update the products list
    LaunchedEffect(Unit) {
        dbRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                products.clear()
                for (productSnapshot in snapshot.children) {
                    val product = productSnapshot.getValue(Product::class.java)
                    product?.let { products.add(it) }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(context, "Failed to load products", Toast.LENGTH_SHORT).show()
            }
        })
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        TextField(
            value = productName,
            onValueChange = { productName = it },
            label = { Text("Product Name") },
            modifier = Modifier.fillMaxWidth()
        )
        TextField(
            value = productPrice,
            onValueChange = { productPrice = it },
            label = { Text("Product Price") },
            modifier = Modifier.fillMaxWidth()
        )
        Button(
            onClick = {
                val price = productPrice.toDoubleOrNull()
                if (productName.isNotEmpty() && price != null) {
                    val productId = dbRef.push().key ?: return@Button
                    val product = Product(productId, productName, price)

                    dbRef.child(productId).setValue(product).addOnSuccessListener {
                        Toast.makeText(context, "Product added successfully", Toast.LENGTH_SHORT).show()
                    }.addOnFailureListener {
                        Toast.makeText(context, "Failed to add product", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(context, "Please enter a valid name and price", Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Add Product")
        }
        Button(
            onClick = {
                // Update Logic (select product and update it)
                if (productName.isNotEmpty() && productPrice.isNotEmpty()) {
                    val price = productPrice.toDoubleOrNull()
                    val productToUpdate = products.find { it.name == productName }
                    if (productToUpdate != null && price != null) {
                        val updatedProduct = productToUpdate.copy(name = productName, price = price)
                        dbRef.child(productToUpdate.id).setValue(updatedProduct).addOnSuccessListener {
                            Toast.makeText(context, "Product updated successfully", Toast.LENGTH_SHORT).show()
                        }.addOnFailureListener {
                            Toast.makeText(context, "Failed to update product", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(context, "Product not found or invalid price", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Update Product")
        }
        Button(
            onClick = {
                // Delete Logic (select product and delete it)
                val productToDelete = products.find { it.name == productName }
                if (productToDelete != null) {
                    dbRef.child(productToDelete.id).removeValue().addOnSuccessListener {
                        Toast.makeText(context, "Product deleted successfully", Toast.LENGTH_SHORT).show()
                    }.addOnFailureListener {
                        Toast.makeText(context, "Failed to delete product", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(context, "Product not found", Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Delete Product")
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "Product List:")
        products.forEach { product ->
            Text(text = "${product.name}: $${product.price}", modifier = Modifier.padding(4.dp))
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ProductScreenPreview() {
    MyApplicationTheme {
        ProductScreen(FirebaseDatabase.getInstance().getReference("products"))
    }
}
