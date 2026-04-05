#![cfg_attr(debug_assertions, allow(dead_code, unused_imports))]
use tauri::{Manager, Window};

fn main() {
  tauri::Builder::default()
    .invoke_handler(tauri::generate_handler![
      read_dir,
      read_file,
      write_file,
      create_dir,
      remove_file,
      rename_file
    ])
    .run(tauri::generate_context!())
    .expect("error while running tauri application");
}

#[tauri::command]
async fn read_dir(path: String) -> Result<serde_json::Value, String> {
  use std::fs;
  use std::path::Path;
  
  let path = Path::new(&path);
  if !path.exists() || !path.is_dir() {
    return Err("Directory does not exist".to_string());
  }
  
  let entries = fs::read_dir(path).map_err(|e| e.to_string())?;
  let mut files = Vec::new();
  
  for entry in entries {
    if let Ok(entry) = entry {
      let path = entry.path();
      let file_name = path.file_name().unwrap_or_default().to_str().unwrap_or("").to_string();
      let is_dir = path.is_dir();
      let metadata = fs::metadata(&path).ok();
      let size = metadata.as_ref().and_then(|m| if m.is_file() { Some(m.len()) } else { None });
      let modified = metadata.as_ref().and_then(|m| m.modified().ok());
      
      files.push(serde_json::json!({
        "name": file_name,
        "path": path.to_str().unwrap_or(""),
        "is_dir": is_dir,
        "size": size,
        "modified": modified.map(|t| t.duration_since(std::time::UNIX_EPOCH).unwrap().as_secs())
      }));
    }
  }
  
  Ok(serde_json::Value::Array(files))
}

#[tauri::command]
async fn read_file(path: String) -> Result<String, String> {
  use std::fs;
  fs::read_to_string(&path).map_err(|e| e.to_string())
}

#[tauri::command]
async fn write_file(path: String, content: String) -> Result<(), String> {
  use std::fs;
  fs::write(&path, content).map_err(|e| e.to_string())
}

#[tauri::command]
async fn create_dir(path: String) -> Result<(), String> {
  use std::fs;
  fs::create_dir_all(&path).map_err(|e| e.to_string())
}

#[tauri::command]
async fn remove_file(path: String) -> Result<(), String> {
  use std::fs;
  fs::remove_file(&path).map_err(|e| e.to_string())
}

#[tauri::command]
async fn rename_file(from: String, to: String) -> Result<(), String> {
  use std::fs;
  fs::rename(&from, &to).map_err(|e| e.to_string())
}
